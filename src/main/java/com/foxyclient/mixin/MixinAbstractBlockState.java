package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.XRay;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MixinAbstractBlockState {

    @Shadow public abstract net.minecraft.block.Block getBlock();

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void onGetRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            // Natively drop the mesh if the user wants 0.0 opacity for hidden blocks
            if (!xray.shouldRender(getBlock()) && xray.opacity.get() <= 0.0) {
                cir.setReturnValue(BlockRenderType.INVISIBLE);
            }
        }
    }

    // getLuminance override removed because it crashes the LightStorage engine (NPE on missing ChunkNibbleArrays)

    @Inject(method = "isOpaqueFullCube", at = @At("HEAD"), cancellable = true)
    private void onIsOpaqueFullCube(CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isOpaque", at = @At("HEAD"), cancellable = true)
    private void onIsOpaque(CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isTransparent", at = @At("HEAD"), cancellable = true)
    private void onIsTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        // If XRay is active, all blocks act translucent so we can see through them if Opacity > 0
        if (xray != null && xray.isEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
