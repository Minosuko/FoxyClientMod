package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.XRay;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidRenderer.class)
public class MixinFluidRenderer {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockRenderView world, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            if (!xray.showLiquids.get()) {
                ci.cancel();
            }
        }
    }
}
