package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.util.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        RotationManager.update();
        FoxyClient.INSTANCE.getEventBus().post(TickEvent.INSTANCE);
    }

    private float originalYaw, originalPitch;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPacketsHead(CallbackInfo ci) {
        if (RotationManager.isActive()) {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            originalYaw = player.getYaw();
            originalPitch = player.getPitch();
            
            player.setYaw(RotationManager.getServerYaw());
            player.setPitch(RotationManager.getServerPitch());
        }
    }

    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void onSendMovementPacketsReturn(CallbackInfo ci) {
        if (RotationManager.isActive()) {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            player.setYaw(originalYaw);
            player.setPitch(originalPitch);
        }
    }
}
