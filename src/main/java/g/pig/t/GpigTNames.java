package g.pig.t;

import java.util.List;

/**
 * The single place to add GpigT names. Add or remove entries here —
 * {@link g.pig.t.entity.GpigTEntity} picks from this list on spawn.
 */
public final class GpigTNames {
    private GpigTNames() {
    }

    // TODO: might add per-name weighting / rarity here later so some names
    // show up more often than others.
    public static final List<String> ALL = List.of(
            "Sir Oinksalot",
            "Hammington",
            "Truffle",
            "Bacon Bit",
            "Snortimer",
            "Lord Squeals",
            "Porky",
            "Mudpuddle",
            "Curly",
            "Princess Trotter"
    );
}
