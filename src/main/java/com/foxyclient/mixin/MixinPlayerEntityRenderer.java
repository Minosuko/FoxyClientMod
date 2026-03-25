package com.foxyclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {

    @Inject(method = "updateRenderState", at = @At("RETURN"))
    private void onUpdateRenderState(PlayerLikeEntity player, PlayerEntityRenderState state, float f, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Apply to local player or FakePlayer/Mannequin
        if (player == mc.player || (player instanceof ClientPlayerLikeEntity && player.getId() == -1337)) {
            if (state.skinTextures != null && state.skinTextures.cape() != null) {
                state.capeVisible = true;
            }
        }
    }

    @Inject(method = "updateCape", at = @At("TAIL"))
    private void onUpdateCape(PlayerLikeEntity player, PlayerEntityRenderState state, float tickProgress, CallbackInfo ci) {
        com.foxyclient.module.render.CapePhysics module = com.foxyclient.FoxyClient.INSTANCE.getModuleManager().getModule(com.foxyclient.module.render.CapePhysics.class);
        if (module == null || !module.isEnabled()) return;

        float intensity = module.intensity.get().floatValue();
        float gravity = module.gravity.get().floatValue();

        // field_53536 = Vertical (clamped -6 to 32)
        // field_53537 = Horizontal (clamped 0 to 150)
        // field_53538 = Sideways (clamped -20 to 20)

        state.field_53536 += gravity;
        state.field_53537 *= intensity;
        state.field_53538 *= intensity;

        if (module.smooth.get()) {
            // Apply slight damping/smoothing if needed
            state.field_53537 = net.minecraft.util.math.MathHelper.clamp(state.field_53537, 0.0f, 150.0f);
        }
    }
}
