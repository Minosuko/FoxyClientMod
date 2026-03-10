package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.Fullbright;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into LightmapTextureManager to override the gamma value
 * in the lightmap shader UBO when the Fullbright module is active.
 *
 * Strategy: At the HEAD of update(), we temporarily override the gamma
 * option's value to our high value (bypassing the [0,1] validator).
 * At RETURN, we restore the original value.
 */
@Mixin(LightmapTextureManager.class)
public class MixinLightmapTextureManager {

    @Shadow
    private boolean dirty;

    @Shadow
    private net.minecraft.client.MinecraftClient client;

    @Unique
    private Double foxyclient_savedGamma = null;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateHead(float tickProgress, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;

        Fullbright fullbright = FoxyClient.INSTANCE.getModuleManager().getModule(Fullbright.class);
        if (fullbright != null && fullbright.isEnabled() && fullbright.shouldOverrideGamma()) {
            // Force the lightmap to re-render every frame while fullbright is active
            this.dirty = true;

            // Save the current gamma value and override it
            SimpleOption<Double> gammaOption = this.client.options.getGamma();
            @SuppressWarnings("unchecked")
            SimpleOptionAccessor<Double> accessor = (SimpleOptionAccessor<Double>) (Object) gammaOption;
            foxyclient_savedGamma = accessor.foxyclient_getValue();
            accessor.foxyclient_setValue(fullbright.getGammaOverride());
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void onUpdateReturn(float tickProgress, CallbackInfo ci) {
        if (foxyclient_savedGamma != null) {
            // Restore the original gamma value after the lightmap has been computed
            SimpleOption<Double> gammaOption = this.client.options.getGamma();
            @SuppressWarnings("unchecked")
            SimpleOptionAccessor<Double> accessor = (SimpleOptionAccessor<Double>) (Object) gammaOption;
            accessor.foxyclient_setValue(foxyclient_savedGamma);
            foxyclient_savedGamma = null;
        }
    }
}
