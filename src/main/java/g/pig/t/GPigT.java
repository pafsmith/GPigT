package g.pig.t;

import g.pig.t.names.NamesReloadListener;
import g.pig.t.registry.GPigTEntities;
import g.pig.t.registry.GPigTItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public final class GPigT implements ModInitializer {
    public static final String MOD_ID = "gpigt";

    @Override
    public void onInitialize() {
        GPigTEntities.initialize();
        GPigTItems.initialize();
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new NamesReloadListener());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
