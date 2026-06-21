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

    // name, weight (higher = more common)
    public static final List<Map.Entry<String, Integer>> ALL = List.of(
            Map.entry("AIfred Piggyworth", 100),
            Map.entry("Lil' Pig", 100),
            Map.entry("Mr. Smith", 1),
            Map.entry("Hamthropic", 100),
            Map.entry("Boaracle", 100),
            Map.entry("Rasher", 100),
            Map.entry("Sam Pigman", 100),
            Map.entry("Mustafa Piggyman", 100),
            Map.entry("Peter Porker", 100),
            Map.entry("Kevin Bacon", 100),
            Map.entry("Chris P. Bacon", 100),
            Map.entry("Steven Fried", 100),
            Map.entry("John Pork (Is Calling)", 100),
            Map.entry("Sylvester Styllone", 100),
            Map.entry("Bear Grills", 100)
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
