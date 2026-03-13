package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.render.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null) {
            // Post a RenderEvent with null matrices/vcp just for per-frame updates before Camera.update
            FoxyClient.INSTANCE.getEventBus().post(new RenderEvent(null, null, tickCounter.getTickProgress(false)));
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = Zoom.get();
        if (zoom != null && zoom.getMagnification() > 1.0) {
            cir.setReturnValue((float) (cir.getReturnValue() / zoom.getMagnification()));
        }
    }
}
