package g.pig.t.entity;

import g.pig.t.registry.GPigTEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class HogzillaEntity extends AuraImmunePig {

    // --- Size & health ---
    private static final double MAX_HEALTH            = 500.0;
    private static final double MOVEMENT_SPEED_ATTR   = 0.1;
    private static final double KNOCKBACK_RESISTANCE  = 1.0;
    // Scales both the rendered pig model and the hitbox uniformly. A 0.9-block
    // pig at 8x ≈ 7 blocks tall — well past the Warden. HUUUUGE.
    private static final double SCALE                 = 8.0;

    // --- Stalk movement (direct deltaMovement, no pathfinding) ---
    // 0.01 blocks/tick = 0.2 blocks/s = 1 block/5 s
    private static final double STALK_SPEED           = 0.01;
    private static final double STALK_RANGE           = 256.0;
    // Damp vertical component so HOGZ climbs/descends more slowly than it advances horizontally
    private static final double STALK_Y_SCALE         = 0.5;

    // --- Spawn storm ---
    private static final int    SPAWN_STORM_BOLTS     = 30;
    private static final int    SPAWN_STORM_RADIUS    = 30;

    // --- Aura: lightning bolts ---
    private static final int    BOLT_PERIOD_TICKS     = 60;
    private static final int    BOLT_COUNT_MIN        = 5;
    private static final int    BOLT_COUNT_MAX        = 10;
    private static final int    BOLT_RADIUS           = 20;

    // --- Aura: TNT salvo ---
    private static final int    SALVO_PERIOD_TICKS    = 160;
    private static final int    SALVO_COUNT_MIN       = 8;
    private static final int    SALVO_COUNT_MAX       = 12;
    private static final int    SALVO_FUSE_TICKS      = 80;
    private static final double SALVO_SPEED_MIN       = 0.8;
    private static final double SALVO_SPEED_MAX       = 1.2;

    // --- Aura: Porkzillary Forces ---
    private static final int    PORK_PERIOD_TICKS     = 300;
    private static final int    PORK_COUNT_MIN        = 4;
    private static final int    PORK_COUNT_MAX        = 6;
    private static final int    PORK_SPAWN_RADIUS     = 8;

    private final ServerBossEvent bossBar = new ServerBossEvent(
            getUUID(),
            Component.literal("HOGZILLA"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private int spawnStormTicksRemaining = 0;

    public HogzillaEntity(EntityType<? extends Pig> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Pig.createAttributes()
                .add(Attributes.MAX_HEALTH,           MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED,       MOVEMENT_SPEED_ATTR)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
                .add(Attributes.SCALE,                SCALE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new StalkerGoal(this));
    }

    // --- Boss bar ---

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    // --- Spawn ---

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, spawnGroupData);
        spawnStormTicksRemaining = SPAWN_STORM_BOLTS;
        return data;
    }

    // --- Tick ---

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            bossBar.setProgress(getHealth() / getMaxHealth());
            tickUnstoppable();
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel serverLevel) {
        super.customServerAiStep(serverLevel);
        tickSpawnStorm();
        tickAura();
    }

    // --- Spawn storm ---

    private void tickSpawnStorm() {
        if (spawnStormTicksRemaining <= 0) return;
        spawnStormTicksRemaining--;
        spawnLightningAt(SPAWN_STORM_RADIUS);
    }

    // --- Aura ---

    private void tickAura() {
        if (tickCount % BOLT_PERIOD_TICKS == 0)  tickAuraBolts();
        if (tickCount % SALVO_PERIOD_TICKS == 0) tickAuraSalvo();
        if (tickCount % PORK_PERIOD_TICKS == 0)  tickAuraPork();
    }

    private void tickAuraBolts() {
        int count = BOLT_COUNT_MIN + getRandom().nextInt(BOLT_COUNT_MAX - BOLT_COUNT_MIN + 1);
        for (int i = 0; i < count; i++) {
            spawnLightningAt(BOLT_RADIUS);
        }
    }

    private void spawnLightningAt(int radius) {
        double ox = (getRandom().nextDouble() * 2 - 1) * radius;
        double oz = (getRandom().nextDouble() * 2 - 1) * radius;
        BlockPos pos = BlockPos.containing(getX() + ox, getY(), getZ() + oz);
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level());
        bolt.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level().addFreshEntity(bolt);
    }

    private void tickAuraSalvo() {
        ServerLevel serverLevel = (ServerLevel) level();
        int count = SALVO_COUNT_MIN + getRandom().nextInt(SALVO_COUNT_MAX - SALVO_COUNT_MIN + 1);
        for (int i = 0; i < count; i++) {
            double theta = getRandom().nextDouble() * 2 * Math.PI;
            double phi   = Math.acos(2 * getRandom().nextDouble() - 1);
            double vx    = Math.sin(phi) * Math.cos(theta);
            double vy    = Math.cos(phi);
            double vz    = Math.sin(phi) * Math.sin(theta);
            double speed = SALVO_SPEED_MIN + getRandom().nextDouble() * (SALVO_SPEED_MAX - SALVO_SPEED_MIN);

            PrimedTnt tnt = new PrimedTnt(serverLevel, getX(), getEyeY(), getZ(), null);
            tnt.setFuse(SALVO_FUSE_TICKS);
            tnt.setDeltaMovement(vx * speed, vy * speed, vz * speed);
            serverLevel.addFreshEntity(tnt);
        }
    }

    private void tickAuraPork() {
        int count = PORK_COUNT_MIN + getRandom().nextInt(PORK_COUNT_MAX - PORK_COUNT_MIN + 1);
        for (int i = 0; i < count; i++) {
            double ox = (getRandom().nextDouble() * 2 - 1) * PORK_SPAWN_RADIUS;
            double oz = (getRandom().nextDouble() * 2 - 1) * PORK_SPAWN_RADIUS;
            PorkzillaryForceEntity piglet = new PorkzillaryForceEntity(GPigTEntities.PORKZILLARY_FORCE, level());
            piglet.snapTo(getX() + ox, getY(), getZ() + oz, getRandom().nextFloat() * 360F, 0F);
            level().addFreshEntity(piglet);
        }
    }

    // --- Unstoppable ---

    private void tickUnstoppable() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 0.000001) return;

        double nx = getX() + motion.x;
        double ny = getY() + motion.y;
        double nz = getZ() + motion.z;

        destroyIfNotBrick(BlockPos.containing(nx, ny, nz));
        destroyIfNotBrick(BlockPos.containing(nx, getEyeY() + motion.y, nz));
    }

    private void destroyIfNotBrick(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.BRICK_WALL)) {
            level().destroyBlock(pos, false);
        }
    }

    // --- Stalk goal (inner) ---

    private static final class StalkerGoal extends Goal {

        private final HogzillaEntity hogz;

        StalkerGoal(HogzillaEntity hogz) {
            this.hogz = hogz;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() { return true; }

        @Override
        public boolean canContinueToUse() { return true; }

        @Override
        public void tick() {
            Player target = hogz.level().getNearestPlayer(hogz, STALK_RANGE);
            if (target == null) {
                hogz.setDeltaMovement(Vec3.ZERO);
                return;
            }

            double dx  = target.getX() - hogz.getX();
            double dy  = target.getY() - hogz.getY();
            double dz  = target.getZ() - hogz.getZ();
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (len < 0.5) {
                hogz.setDeltaMovement(Vec3.ZERO);
                return;
            }

            double scale = STALK_SPEED / len;
            hogz.setDeltaMovement(dx * scale, dy * scale * STALK_Y_SCALE, dz * scale);
            hogz.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        }
    }
}
