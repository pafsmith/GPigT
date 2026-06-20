package g.pig.t;

import g.pig.t.registry.GPigTEntities;
import g.pig.t.registry.GPigTItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class GPigT implements ModInitializer {
    public static final String MOD_ID = "gpigt";

    @Override
    public void onInitialize() {
        GPigTEntities.initialize();
        GPigTItems.initialize();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
