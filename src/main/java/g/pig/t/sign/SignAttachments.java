package g.pig.t.sign;

import g.pig.t.GPigT;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Objects;

public final class SignAttachments {
    private static final AttachmentType<SignState> SIGN_STATE = AttachmentRegistry.create(
            GPigT.id("sign_state"),
            builder -> builder
                    .initializer(() -> SignState.NONE)
                    .persistent(SignState.CODEC)
    );

    private SignAttachments() {
    }

    public static void initialize() {
        // Loads the class so static attachment registrations run during mod initialization.
    }

    public static SignState getState(SignBlockEntity sign) {
        return ((AttachmentTarget) sign).getAttachedOrElse(SIGN_STATE, SignState.NONE);
    }

    public static void setState(SignBlockEntity sign, SignState state) {
        Objects.requireNonNull(state, "state");
        AttachmentTarget target = (AttachmentTarget) sign;
        if (state == SignState.NONE) {
            target.removeAttached(SIGN_STATE);
            return;
        }
        target.setAttached(SIGN_STATE, state);
    }
}
