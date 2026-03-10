package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.PacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    // ThreadLocal to hold the event so both modifySendPacket and onSendPacket can share it
    // Wait, simpler: just let ModifyVariable return the new packet, and another Inject for cancel.
    // Instead of posting twice, we can just mixin to "send" and replace it, and cancel if the event is cancelled.
    private PacketEvent.Send currentSendEvent;

    @ModifyVariable(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> modifySendPacket(Packet<?> packet) {
        if (FoxyClient.INSTANCE == null) return packet;
        currentSendEvent = new PacketEvent.Send(packet);
        FoxyClient.INSTANCE.getEventBus().post(currentSendEvent);
        return currentSendEvent.getPacket(); // Allow event handlers to replace the packet
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (currentSendEvent != null && currentSendEvent.isCancelled()) {
            ci.cancel();
        }
        currentSendEvent = null; // Reset
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        PacketEvent.Receive event = new PacketEvent.Receive(packet);
        FoxyClient.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) ci.cancel();
    }
}
