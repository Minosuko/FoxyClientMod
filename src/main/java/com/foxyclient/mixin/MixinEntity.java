package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.movement.NoSlow;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {
    @Inject(method = "slowMovement", at = @At("HEAD"), cancellable = true)
    private void onSlowMovement(BlockState state, Vec3d multiplier, CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null) {
            NoSlow noSlow = FoxyClient.INSTANCE.getModuleManager().getModule(NoSlow.class);
            if (noSlow != null && noSlow.isEnabled()) {
                if (state.isOf(Blocks.SOUL_SAND) && noSlow.noSoulSand()) {
                    ci.cancel();
                } else if (state.isOf(Blocks.HONEY_BLOCK) && noSlow.noHoney()) {
                    ci.cancel();
                } else if (state.isOf(Blocks.COBWEB) && noSlow.noWebs()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        com.foxyclient.module.render.Freecam freecam = com.foxyclient.module.render.Freecam.get();
        if (freecam != null && freecam.isEnabled()) {
            freecam.updateRotation(cursorDeltaX, cursorDeltaY);
            ci.cancel();
            return;
        }

        com.foxyclient.module.render.Freelook freelook = com.foxyclient.module.render.Freelook.get();
        if (freelook != null && freelook.isEnabled()) {
            freelook.updateCamera(cursorDeltaX, cursorDeltaY);
            ci.cancel();
            return;
        }
    }
}
