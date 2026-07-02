package g.pig.t;

import g.pig.t.names.NamesReloadListener;
import g.pig.t.ponder.PonderService;
import g.pig.t.registry.GPigTEntities;
import g.pig.t.registry.GPigTItems;
import g.pig.t.sign.SignAttachments;
import g.pig.t.sign.SignDebugCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GPigT implements ModInitializer {
    public static final String MOD_ID = "gpigt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GPigTEntities.initialize();
        GPigTItems.initialize();
        SignAttachments.initialize();
        SignDebugCommands.initialize();
        PonderService.initialize();
        ResourceManagerHelper.get(PackType.SERVER_DATA)
                .registerReloadListener(new NamesReloadListener());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
