package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.util.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class MixinClientCommonNetworkHandler {
    @Unique
    private boolean sendingCorrection = false;

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (sendingCorrection || FoxyClient.INSTANCE == null || MinecraftClient.getInstance().player == null) return;
        
        if (RotationManager.isActive()) {
            boolean shouldCorrect = false;
            
            if (packet instanceof PlayerActionC2SPacket actionPacket) {
                if (actionPacket.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                    shouldCorrect = isAimingItem();
                }
            } else if (packet instanceof PlayerInteractItemC2SPacket) {
                shouldCorrect = isAimingItem();
            }
            
            if (shouldCorrect) {
                MinecraftClient mc = MinecraftClient.getInstance();
                sendingCorrection = true;
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    mc.player.getYaw(), 
                    mc.player.getPitch(), 
                    mc.player.isOnGround(),
                    mc.player.horizontalCollision
                ));
                sendingCorrection = false;
            }
        }
    }

    @Unique
    private boolean isAimingItem() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        // Check active item if using, or just held item if interacting
        net.minecraft.item.ItemStack stack = mc.player.getActiveItem();
        if (stack.isEmpty()) stack = mc.player.getMainHandStack();
        
        net.minecraft.item.Item item = stack.getItem();
        return item instanceof net.minecraft.item.BowItem || 
               item instanceof net.minecraft.item.CrossbowItem || 
               item instanceof net.minecraft.item.TridentItem;
    }
}
