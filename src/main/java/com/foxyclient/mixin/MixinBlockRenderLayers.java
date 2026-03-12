package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.XRay;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderLayers.class)
public class MixinBlockRenderLayers {

    @Inject(method = "getBlockLayer", at = @At("HEAD"), cancellable = true)
    private static void onGetBlockLayer(BlockState state, CallbackInfoReturnable<BlockRenderLayer> cir) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            if (!xray.shouldRender(state.getBlock()) && xray.opacity.get() > 0.0) {
                cir.setReturnValue(BlockRenderLayer.TRANSLUCENT);
            }
        }
    }
}
