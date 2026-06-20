package g.pig.t.client;

import g.pig.t.registry.GPigTEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.PigRenderer;

public class GPigTClient implements ClientModInitializer {
	@Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(GPigTEntities.GPIGT, PigRenderer::new);
    }
}