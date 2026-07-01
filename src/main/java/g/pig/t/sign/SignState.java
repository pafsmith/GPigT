package g.pig.t.sign;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum SignState implements StringRepresentable {
    NONE("none"),
    PROMPT("prompt"),
    CLAIMED("claimed"),
    RESPONSE("response");

    public static final Codec<SignState> CODEC = StringRepresentable.fromEnum(SignState::values);

    private final String serializedName;

    SignState(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public static SignState fromSerializedName(String raw) {
        for (SignState state : values()) {
            if (state.serializedName.equals(raw)) {
                return state;
            }
        }
        return null;
    }
}
