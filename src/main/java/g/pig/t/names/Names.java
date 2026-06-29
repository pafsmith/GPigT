package g.pig.t.names;

import java.util.List;

/**
 * Holds the names GpigT entities can spawn with. Data is loaded from
 * data packs by {@link NamesReloadListener} — add or override names by
 * editing data/gpigt/gpigt_names/*.json, not by changing this class.
 */
public final class Names {
    private Names() {
    }

    /**
     * One entry: a name and its {@link Rarity}. The spawn weight is derived
     * from the rarity rather than stored, so {@link #weight()} keeps working
     * for the pick logic. A missing/unknown rarity falls back to COMMON.
     */
    public record WeightedName(String name, Rarity rarity) {
        public int weight() {
            return rarity == null ? Rarity.COMMON.weight() : rarity.weight();
        }
    }

    private static volatile List<WeightedName> all = List.of();
    private static volatile int totalWeight = 0;

    /** Replaces the loaded names; called by the reload listener on (re)load. */
    public static void setNames(List<WeightedName> names) {
        all = List.copyOf(names);
        int sum = 0;
        for (WeightedName name : all) {
            sum += name.weight();
        }
        totalWeight = sum;
    }

    public static int totalWeight() {
        return totalWeight;
    }

    /**
     * Picks a name for a weighted roll in {@code [0, totalWeight())}.
     * Returns null if no names are loaded so the entity can spawn unnamed
     * rather than crash.
     */
    public static String pick(int roll) {
        for (WeightedName name : all) {
            roll -= name.weight();
            if (roll < 0) {
                return name.name();
            }
        }
        // No names, or a roll outside [0, totalWeight): no name.
        return null;
    }
}
