package g.pig.t;

import java.util.List;
import java.util.Map;

/**
 * The single place to add GpigT names. Add or remove entries here —
 * {@link g.pig.t.entity.GpigTEntity} picks from this list on spawn.
 */
public final class GpigTNames {
    private GpigTNames() {
    }

    // name, weight (higher = more common) — rarity / colour / sound / etc. here ???
    public static final List<Map.Entry<String, Integer>> ALL = List.of(
            Map.entry("Sir Oinksalot", 10),
            Map.entry("Hammington", 10),
            Map.entry("Truffle", 8),
            Map.entry("Bacon Bit", 8),
            Map.entry("Snortimer", 6),
            Map.entry("Lord Squeals", 5),
            Map.entry("Porky", 5),
            Map.entry("Mudpuddle", 4),
            Map.entry("Curly", 3),
            Map.entry("Princess Trotter", 1)
    );

    public static final int TOTAL_WEIGHT = ALL.stream().mapToInt(Map.Entry::getValue).sum();

    /**
     * Picks a name for a weighted roll in {@code [0, TOTAL_WEIGHT)}.
     * Returns null if the list is empty so the entity can spawn unnamed
     * rather than crash.
     */
    public static String pick(int roll) {
        for (Map.Entry<String, Integer> entry : ALL) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        // Empty list, or a roll outside [0, TOTAL_WEIGHT): no name.
        return null;
    }
}
