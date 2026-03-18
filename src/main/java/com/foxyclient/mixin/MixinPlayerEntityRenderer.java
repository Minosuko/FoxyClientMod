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
}
