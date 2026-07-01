package g.pig.t.client.mixin;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forces the title-screen splash to always read "HOGZILLA IS INEVITABLE",
 * overriding the random rotation and every special-case splash (birthday,
 * holidays, merch, etc.).
 */
@Mixin(SplashManager.class)
public class SplashManagerMixin {

    private static final SplashRenderer HOGZILLA_SPLASH =
            new SplashRenderer(Component.literal("HOGZILLA IS INEVITABLE"));

    @Inject(at = @At("HEAD"), method = "getSplash", cancellable = true)
    private void gpigt$inevitable(CallbackInfoReturnable<SplashRenderer> cir) {
        cir.setReturnValue(HOGZILLA_SPLASH);
    }
}
