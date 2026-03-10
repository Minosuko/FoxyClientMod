package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Module;
import com.foxyclient.util.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow public abstract boolean isPlayer();

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void onChangeLookDirection(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        // Freelook override would go here
    }

    @Inject(method = "isLogicalSideForUpdatingMovement", at = @At("HEAD"), cancellable = true)
    private void onIsLogicalSideForUpdatingMovement(CallbackInfoReturnable<Boolean> cir) {
        if (FoxyClient.INSTANCE == null) return;
        Module entityControl = FoxyClient.INSTANCE.getModuleManager().getModule("EntityControl");
        if (entityControl != null && entityControl.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getYaw(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetYaw(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (RotationManager.isActive() && (Object) this == MinecraftClient.getInstance().player) {
            if (!RotationManager.shouldNormalize()) {
                cir.setReturnValue(RotationManager.getInterpolatedYaw(tickDelta));
            }
        }
    }

    @Inject(method = "getPitch(F)F", at = @At("HEAD"), cancellable = true)
    private void onGetPitch(float tickDelta, CallbackInfoReturnable<Float> cir) {
        if (RotationManager.isActive() && (Object) this == MinecraftClient.getInstance().player) {
            if (!RotationManager.shouldNormalize()) {
                cir.setReturnValue(RotationManager.getInterpolatedPitch(tickDelta));
            }
        }
    }
}
