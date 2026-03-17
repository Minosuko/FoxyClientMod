package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;

import com.foxyclient.module.render.Zoom;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = Zoom.get();
        if (zoom != null && zoom.getMagnification() > 1.0) {
            cir.setReturnValue((float) (cir.getReturnValue() / zoom.getMagnification()));
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", shift = At.Shift.AFTER))
    private void onRenderWorldAfter(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        com.foxyclient.module.render.MotionBlur module = com.foxyclient.module.render.MotionBlur.INSTANCE;
        if (module != null && module.isEnabled() && module.getBlur() > 0.0f) {
            float strength = module.getBlur();
            module.getShader().setBlendFactor(strength);
            
            // Set additional config from screen size
            module.getShader().setViewRes((float) client.getWindow().getFramebufferWidth(), (float) client.getWindow().getFramebufferHeight());
            
            // Basic sample scaling based on strength mapping (we avoid doing the complex FPS scaling of reference mod for simplicity, just a static 12 samples for now)
            module.getShader().setMotionBlurSamples(12);
            module.getShader().setHalfSamples(6);
            module.getShader().setInverseSamples(1.0f / 12.0f);
            module.getShader().setBlurAlgorithm(1); // 1 = center blur

            module.getShader().render(tickCounter.getTickProgress(true));
        }
    }
}
