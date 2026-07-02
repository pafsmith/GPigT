package g.pig.t.sign;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

import java.util.List;
import java.util.Objects;

/**
 * The one place allowed to read or write a sign. Every operation resolves the
 * sign from its position and is a safe no-op if the block is no longer a sign,
 * so callers can hold a {@link BlockPos} across ticks without extra guarding.
 *
 * <p>All methods are server-thread only — the {@link ServerLevel} parameter both
 * enforces that (no client access) and provides the client-sync channel used
 * after a write. Sign state is delegated to {@link SignAttachments}, so this
 * class is the single facade over both a sign's text and its {@link SignState}.
 */
public final class SignIO {
    private SignIO() {
    }

    /**
     * The front-face text as one combined string: non-blank lines joined by a
     * single space. Returns {@code ""} if the sign is gone or blank.
     */
    public static String readFrontText(ServerLevel level, BlockPos pos) {
        SignBlockEntity sign = signAt(level, pos);
        if (sign == null) {
            return "";
        }
        StringBuilder combined = new StringBuilder();
        for (Component message : sign.getFrontText().getMessages(false)) {
            String line = message.getString().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (combined.length() > 0) {
                combined.append(' ');
            }
            combined.append(line);
        }
        return combined.toString();
    }

    /**
     * Overwrites the front face with up to {@link SignText#LINES} lines (extra
     * lines are dropped, missing lines cleared) and syncs the change to clients.
     * No-op if the sign is gone.
     */
    public static void writeFrontText(ServerLevel level, BlockPos pos, List<Component> lines) {
        Objects.requireNonNull(lines, "lines");
        SignBlockEntity sign = signAt(level, pos);
        if (sign == null) {
            return;
        }
        sign.updateText(text -> applyLines(text, lines), true);
        sign.setChanged();
        level.sendBlockUpdated(pos, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_ALL);
    }

    /** The sign's {@link SignState}; {@link SignState#NONE} if the sign is gone. */
    public static SignState getState(ServerLevel level, BlockPos pos) {
        SignBlockEntity sign = signAt(level, pos);
        if (sign == null) {
            return SignState.NONE;
        }
        return SignAttachments.getState(sign);
    }

    /** Sets the sign's {@link SignState}. No-op if the sign is gone. */
    public static void setState(ServerLevel level, BlockPos pos, SignState state) {
        Objects.requireNonNull(state, "state");
        SignBlockEntity sign = signAt(level, pos);
        if (sign == null) {
            return;
        }
        SignAttachments.setState(sign, state);
    }

    private static SignText applyLines(SignText text, List<Component> lines) {
        SignText updated = text;
        for (int line = 0; line < SignText.LINES; line++) {
            Component message = line < lines.size() ? lines.get(line) : null;
            updated = updated.setMessage(line, message == null ? Component.empty() : message);
        }
        return updated;
    }

    private static SignBlockEntity signAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SignBlockEntity sign ? sign : null;
    }
}
