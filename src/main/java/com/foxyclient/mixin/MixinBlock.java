package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.render.XRay;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class MixinBlock {

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private static void onShouldDrawSide(BlockState state, BlockState otherState, Direction side, CallbackInfoReturnable<Boolean> circle) {
        if (FoxyClient.INSTANCE == null) return;
        XRay xray = FoxyClient.INSTANCE.getModuleManager().getModule(XRay.class);
        if (xray != null && xray.isEnabled()) {
            boolean isTarget = xray.shouldRender(state.getBlock());
            boolean isOtherTarget = xray.shouldRender(otherState.getBlock());
            
            // Force drawing sides of XRay targets ONLY if the adjacent block is hidden by XRay
            if (isTarget && !isOtherTarget) {
                circle.setReturnValue(true);
            }
        }
    }
}
