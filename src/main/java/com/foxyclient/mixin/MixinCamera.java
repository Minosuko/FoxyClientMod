package com.foxyclient.mixin;

import com.foxyclient.module.render.Freecam;
import com.foxyclient.module.render.Freelook;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(Vec3d pos);

    /**
     * After the vanilla Camera.update finishes, override position and rotation
     * with the Freecam values so the renderer uses the detached camera.
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateTail(net.minecraft.world.World area, net.minecraft.entity.Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        Freecam freecam = Freecam.get();
        if (freecam != null && freecam.isEnabled()) {
            setPos(freecam.getCamPos());
            setRotation(freecam.getCamYaw(), freecam.getCamPitch());
        }
    }

    /**
     * Tell the engine "yes, third-person" so the local player model is rendered.
     */
    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
        Freecam freecam = Freecam.get();
        if (freecam != null && freecam.isEnabled()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Disable camera-clip so the camera can pass through blocks when noclip is on.
     */
    @Inject(method = "clipToSpace", at = @At("HEAD"), cancellable = true)
    private void onClipToSpace(float desiredCameraDistance, CallbackInfoReturnable<Float> cir) {
        Freecam freecam = Freecam.get();
        if (freecam != null && freecam.isEnabled() && (freecam.noclip == null || freecam.noclip.get())) {
            cir.setReturnValue(desiredCameraDistance);
        }
    }

    // ── Freelook redirects (unchanged) ─────────────────────────────────

    @org.spongepowered.asm.mixin.injection.Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F"))
    private float onGetYaw(net.minecraft.entity.Entity entity, float tickDelta) {
        Freelook freelook = Freelook.get();
        if (freelook != null && freelook.isEnabled() && entity == net.minecraft.client.MinecraftClient.getInstance().player) {
            return freelook.getCameraYaw();
        }
        return entity.getYaw(tickDelta);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPitch(F)F"))
    private float onGetPitch(net.minecraft.entity.Entity entity, float tickDelta) {
        Freelook freelook = Freelook.get();
        if (freelook != null && freelook.isEnabled() && entity == net.minecraft.client.MinecraftClient.getInstance().player) {
            return freelook.getCameraPitch();
        }
        return entity.getPitch(tickDelta);
    }
}
