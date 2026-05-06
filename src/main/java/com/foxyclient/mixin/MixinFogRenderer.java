package com.foxyclient.mixin;

import com.foxyclient.module.render.KillEffect;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {

    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true)
    private void onGetFogColor(Camera camera, float tickProgress, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir) {
        float intensity = KillEffect.getKillEffectSkyIntensity(camera);
        if (intensity > 0) {
            Vector4f color = cir.getReturnValue();
            int type = KillEffect.currentBlackoutType;
            
            float targetR = 0, targetG = 0, targetB = 0;
            if (type == 2) { // Aemondir
                targetR = 0.1F; 
            }
            
            float r = net.minecraft.util.math.MathHelper.lerp(intensity, color.x, targetR);
            float g = net.minecraft.util.math.MathHelper.lerp(intensity, color.y, targetG);
            float b = net.minecraft.util.math.MathHelper.lerp(intensity, color.z, targetB);
            
            cir.setReturnValue(new Vector4f(r, g, b, color.w));
        }
    }

    @ModifyArgs(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;", 
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void onApplyFogModifyArgs(Args args) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;
        
        float intensity = KillEffect.getKillEffectSkyIntensity(mc.gameRenderer.getCamera());
        if (intensity >= 0.5f) {
            // Index 5: renderDistanceStart
            // Index 6: renderDistanceEnd
            // Ensure the fog doesn't cut off the stadium crowd (25m) or the demonic blade.
            float currentEnd = args.get(6);
            if (currentEnd < 40.0f) {
                args.set(6, 40.0f);
            }
            float currentStart = args.get(5);
            if (currentStart < 35.0f) {
                args.set(5, 35.0f);
            }
        }
    }
}
