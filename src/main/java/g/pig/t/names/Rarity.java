package g.pig.t.names;

import net.minecraft.ChatFormatting;

/**
 * Spawn rarity for a GpigT name. Each tier owns its relative spawn weight
 * (higher = more common) and its display colour, so names inherit both by
 * referencing a tier rather than hardcoding them. Tune the balance or palette
 * by editing the tiers here — it applies to every name of that tier at once.
 *
 * <p>Loaded from data packs by name (e.g. {@code "rarity": "LEGENDARY"}); see
 * {@link NamesReloadListener}.
 */
public enum Rarity {
    COMMON(100, ChatFormatting.WHITE),
    RARE(25, ChatFormatting.AQUA),
    EPIC(5, ChatFormatting.LIGHT_PURPLE),
    LEGENDARY(1, ChatFormatting.GOLD),
    HEROSWINE(50, ChatFormatting.DARK_RED);

    private final int weight;
    private final ChatFormatting color;

    /** Relative spawn weight for names of this tier. */
    Rarity(int weight, ChatFormatting color) {
        this.weight = weight;
        this.color = color;
    }

    /** Relative spawn weight for names of this tier. */
    public int weight() {
        return weight;
    }

    /** Colour the name renders in for this tier. */
    public ChatFormatting color() {
        return color;
    }

    /**
     * Resolves a rarity by its exact name (e.g. {@code "EPIC"}), or null if
     * {@code raw} is null or doesn't match a tier. Callers treat null as
     * invalid data — log and skip — rather than guessing a default.
     */
    public static Rarity fromString(String raw) {
        if (raw == null) {
            return null;
        }
        for (Rarity rarity : values()) {
            if (rarity.name().equals(raw)) {
                return rarity;
            }
        }
        return null;
    }
}