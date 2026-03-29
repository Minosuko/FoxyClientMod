package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.AttackEntityEvent;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null && player.getEntityWorld().isClient()) {
            FoxyClient.INSTANCE.getEventBus().post(new AttackEntityEvent(target));
        }
    }
}
