package g.pig.t.names;

/**
 * Spawn rarity for a GpigT name. Each tier owns its relative spawn weight
 * (higher = more common), so names inherit a weight by referencing a tier
 * rather than hardcoding a number. Tune the balance by editing the weights
 * here — it applies to every name of that tier at once.
 *
 * <p>Loaded from data packs by name (e.g. {@code "rarity": "LEGENDARY"}); see
 * {@link NamesReloadListener}.
 */
public enum Rarity {
    COMMON(100),
    RARE(25),
    EPIC(5),
    LEGENDARY(1);

    private final int weight;

    Rarity(int weight) {
        this.weight = weight;
    }

    /** Relative spawn weight for names of this tier. */
    public int weight() {
        return weight;
    }
}
