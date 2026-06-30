package g.pig.t.client;

import g.pig.t.GPigT;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.resources.Identifier;

public class GpigTRenderer extends PigRenderer {
    private static final Identifier TEXTURE = GPigT.id("textures/entity/gpigt.png");

    public GpigTRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(PigRenderState renderState) {
        return TEXTURE;
    }
}
