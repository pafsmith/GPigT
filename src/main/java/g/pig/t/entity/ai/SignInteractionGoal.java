package g.pig.t.entity.ai;

import g.pig.t.GPigT;
import g.pig.t.entity.GPigTEntity;
import g.pig.t.ponder.PonderService;
import g.pig.t.sign.SignIO;
import g.pig.t.sign.SignState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sends a GPigT to answer the nearest sign after it has been fed a carrot.
 *
 * <h2>State machine</h2>
 *
 * <p>The FSM lives entirely in this goal; the entity holds only the
 * {@code huntRequested} trigger flag, set by {@code mobInteract}.
 *
 * <pre>
 * From → To          | Condition / trigger       | Action
 * -------------------|---------------------------|------------------------------------------------
 * (start) → HUNTING  | goal starts               | clear huntRequested, path to the target sign
 * HUNTING → READING  | within arrival distance   | stop navigation
 * HUNTING → IDLE     | sign invalid / timeout    | abort
 * READING → THINKING | text read                 | log the ponder line once; launch async ponder
 * THINKING (tick)    | pondering                 | spin in place, poll the ponder future
 * THINKING → WRITING | ponder result available   | —
 * WRITING → IDLE     | sign still valid          | write answer to front face, mark RESPONSE
 * WRITING → IDLE     | sign invalid              | abort, no write
 * IDLE               | —                         | canContinueToUse false → stop()
 * </pre>
 *
 * <p>A sign is a valid target while its front text is non-blank and its
 * {@link SignState} is not {@link SignState#RESPONSE} — an answered sign is
 * never answered again. Validity is rechecked every tick while hunting and
 * again immediately before writing, so a sign that is broken, cleared or
 * answered by another GPigT mid-hunt aborts cleanly to IDLE.
 *
 * <h2>Threading</h2>
 *
 * <p>Every transition and every world/entity access runs on the server thread
 * (goals only tick from the server AI step). The only off-thread work is the
 * ponder body inside {@link PonderService}; its {@link CompletableFuture} is
 * polled with {@code getNow(null)} from {@link #tick()} — no callbacks are
 * attached, so a future that completes after the goal is interrupted is inert.
 */
public class SignInteractionGoal extends Goal {
    private static final int HUNT_RADIUS = 32;
    private static final float SPIN_DEGREES_PER_TICK = 30.0F;
    private static final double MOVE_SPEED = 1.2;
    private static final double ARRIVAL_DISTANCE_SQR = 2.0 * 2.0;
    private static final int HUNT_TIMEOUT_TICKS = 20 * 15;
    private static final int REPATH_INTERVAL_TICKS = 20;
    private static final int MAX_LINE_LENGTH = 15;
    private static final String FALLBACK_NAME = "GPigT";

    private enum HuntState {
        HUNTING,
        READING,
        THINKING,
        WRITING,
        IDLE
    }

    private final GPigTEntity pig;
    private HuntState state = HuntState.IDLE;
    private BlockPos targetSign;
    private int huntTicks;
    private CompletableFuture<String> ponderFuture;
    private String answer;

    public SignInteractionGoal(GPigTEntity pig) {
        this.pig = pig;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * The nearest sign the pig may answer: within {@link #HUNT_RADIUS}, with
     * non-blank front text and not already {@link SignState#RESPONSE}. Scans
     * the block-entity maps of loaded chunks only — never forces a load.
     * Also used by {@code GPigTEntity#mobInteract} to fail fast when a carrot
     * is fed with no sign in range.
     */
    public static BlockPos findNearestValidSign(ServerLevel level, GPigTEntity pig) {
        int minSectionX = SectionPos.blockToSectionCoord((int) Math.floor(pig.getX() - HUNT_RADIUS));
        int maxSectionX = SectionPos.blockToSectionCoord((int) Math.floor(pig.getX() + HUNT_RADIUS));
        int minSectionZ = SectionPos.blockToSectionCoord((int) Math.floor(pig.getZ() - HUNT_RADIUS));
        int maxSectionZ = SectionPos.blockToSectionCoord((int) Math.floor(pig.getZ() + HUNT_RADIUS));
        BlockPos best = null;
        double bestDistSqr = (double) HUNT_RADIUS * HUNT_RADIUS;
        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
            for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    if (!(entry.getValue() instanceof SignBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = entry.getKey();
                    double distSqr = pos.distToCenterSqr(pig.getX(), pig.getY(), pig.getZ());
                    if (distSqr > bestDistSqr) {
                        continue;
                    }
                    if (SignIO.readFrontText(level, pos).isEmpty()
                            || SignIO.getState(level, pos) == SignState.RESPONSE) {
                        continue;
                    }
                    best = pos.immutable();
                    bestDistSqr = distSqr;
                }
            }
        }
        return best;
    }

    @Override
    public boolean canUse() {
        if (!this.pig.isHuntRequested()) {
            return false;
        }
        BlockPos found = findNearestValidSign(getServerLevel(this.pig), this.pig);
        if (found == null) {
            // The sign that was in range at feed time is gone — the carrot is
            // wasted, signalled the same way as feeding with no sign in range.
            this.pig.clearHuntRequest();
            this.pig.emitHuntFailureSmoke();
            return false;
        }
        this.targetSign = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.state != HuntState.IDLE;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.state = HuntState.HUNTING;
        this.pig.clearHuntRequest();
        this.huntTicks = 0;
        this.ponderFuture = null;
        this.answer = null;
        Vec3 center = Vec3.atCenterOf(this.targetSign);
        this.pig.getNavigation().moveTo(center.x, center.y, center.z, MOVE_SPEED);
    }

    @Override
    public void stop() {
        this.state = HuntState.IDLE;
        this.targetSign = null;
        this.ponderFuture = null;
        this.answer = null;
        this.pig.getNavigation().stop();
    }

    @Override
    public void tick() {
        ServerLevel level = getServerLevel(this.pig);
        switch (this.state) {
            case HUNTING -> tickHunting(level);
            case READING -> tickReading(level);
            case THINKING -> tickThinking();
            case WRITING -> tickWriting(level);
            case IDLE -> {
            }
        }
    }

    private void tickHunting(ServerLevel level) {
        if (!signStillValid(level)) {
            this.state = HuntState.IDLE;
            return;
        }
        this.huntTicks++;
        if (this.huntTicks > HUNT_TIMEOUT_TICKS) {
            this.state = HuntState.IDLE;
            return;
        }
        Vec3 center = Vec3.atCenterOf(this.targetSign);
        this.pig.getLookControl().setLookAt(center.x, center.y, center.z);
        if (this.pig.distanceToSqr(center.x, center.y, center.z) <= ARRIVAL_DISTANCE_SQR) {
            this.pig.getNavigation().stop();
            this.state = HuntState.READING;
            return;
        }
        if (this.pig.getNavigation().isDone() || this.huntTicks % REPATH_INTERVAL_TICKS == 0) {
            this.pig.getNavigation().moveTo(center.x, center.y, center.z, MOVE_SPEED);
        }
    }

    private void tickReading(ServerLevel level) {
        if (!signStillValid(level)) {
            this.state = HuntState.IDLE;
            return;
        }
        String question = SignIO.readFrontText(level, this.targetSign);
        if (question.isEmpty()) {
            this.state = HuntState.IDLE;
            return;
        }
        Component customName = this.pig.getCustomName();
        String name = customName != null ? customName.getString() : FALLBACK_NAME;
        GPigT.LOGGER.info("{} is pondering the infinite cosmos", name);
        this.ponderFuture = PonderService.ponder(question);
        this.state = HuntState.THINKING;
    }

    private void tickThinking() {
        this.pig.getNavigation().stop();
        float yaw = this.pig.getYRot() + SPIN_DEGREES_PER_TICK;
        this.pig.setYRot(yaw);
        this.pig.setYBodyRot(yaw);
        this.pig.setYHeadRot(yaw);
        if (this.ponderFuture.isCompletedExceptionally()) {
            GPigT.LOGGER.debug("Ponder failed for sign at {}", this.targetSign);
            this.state = HuntState.IDLE;
            return;
        }
        String result = this.ponderFuture.getNow(null);
        if (result != null) {
            this.answer = result;
            this.state = HuntState.WRITING;
        }
    }

    private void tickWriting(ServerLevel level) {
        if (signStillValid(level)) {
            SignIO.writeFrontText(level, this.targetSign, splitIntoLines(this.answer));
            SignIO.setState(level, this.targetSign, SignState.RESPONSE);
        }
        this.state = HuntState.IDLE;
    }

    private boolean signStillValid(ServerLevel level) {
        return !SignIO.readFrontText(level, this.targetSign).isEmpty()
                && SignIO.getState(level, this.targetSign) != SignState.RESPONSE;
    }

    /**
     * Greedy word-wrap onto at most {@link SignText#LINES} sign lines of
     * {@link #MAX_LINE_LENGTH} characters; words longer than a line are
     * hard-split, and overflow past the last line is dropped.
     */
    private static List<Component> splitIntoLines(String answer) {
        List<Component> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : answer.trim().split("\\s+")) {
            while (word.length() > MAX_LINE_LENGTH) {
                int room = line.isEmpty() ? MAX_LINE_LENGTH : MAX_LINE_LENGTH - line.length() - 1;
                if (room > 0) {
                    if (!line.isEmpty()) {
                        line.append(' ');
                    }
                    line.append(word, 0, room);
                    word = word.substring(room);
                }
                lines.add(Component.literal(line.toString()));
                line.setLength(0);
                if (lines.size() >= SignText.LINES) {
                    return lines;
                }
            }
            if (word.isEmpty()) {
                continue;
            }
            if (line.isEmpty()) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= MAX_LINE_LENGTH) {
                line.append(' ').append(word);
            } else {
                lines.add(Component.literal(line.toString()));
                line.setLength(0);
                if (lines.size() >= SignText.LINES) {
                    return lines;
                }
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(Component.literal(line.toString()));
        }
        return lines;
    }
}
