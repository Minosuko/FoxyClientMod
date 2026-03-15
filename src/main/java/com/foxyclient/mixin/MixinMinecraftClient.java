package com.foxyclient.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow public abstract Session getSession();

    @Inject(method = "getGameProfile", at = @At("RETURN"), cancellable = true)
    private void onGetGameProfile(CallbackInfoReturnable<GameProfile> cir) {
        GameProfile original = cir.getReturnValue();
        Session session = this.getSession();
        // If the current session does not match the original cached profile...
        if (original != null && session != null && !original.id().equals(session.getUuidOrNull())) {
            cir.setReturnValue(new GameProfile(session.getUuidOrNull(), session.getUsername()));
        } else if (original == null && session != null) {
            cir.setReturnValue(new GameProfile(session.getUuidOrNull(), session.getUsername()));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.foxyclient.FoxyClient.INSTANCE != null) {
            com.foxyclient.FoxyClient.INSTANCE.onTick();
        }
    }
}
