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

    // ThreadLocal to safely share event between ModifyVariable and Inject on the same call
    private static final ThreadLocal<PacketEvent.Send> currentSendEvent = new ThreadLocal<>();
    private static final ThreadLocal<PacketEvent.Receive> currentReceiveEvent = new ThreadLocal<>();

    @ModifyVariable(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> modifySendPacket(Packet<?> packet) {
        try {
            if (FoxyClient.INSTANCE == null) return packet;
            PacketEvent.Send event = new PacketEvent.Send(packet);
            currentSendEvent.set(event);
            FoxyClient.INSTANCE.getEventBus().post(event);
            return event.getPacket();
        } catch (Exception e) {
            return packet;
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        try {
            PacketEvent.Send event = currentSendEvent.get();
            if (event != null && event.isCancelled()) {
                ci.cancel();
            }
        } finally {
            currentSendEvent.remove();
        }
    }

    @ModifyVariable(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> modifyReceivePacket(Packet<?> packet) {
        try {
            if (FoxyClient.INSTANCE == null) return packet;
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            currentReceiveEvent.set(event);
            FoxyClient.INSTANCE.getEventBus().post(event);
            return event.getPacket();
        } catch (Exception e) {
            return packet;
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        try {
            PacketEvent.Receive event = currentReceiveEvent.get();
            if (event != null && event.isCancelled()) {
                ci.cancel();
            }
        } finally {
            currentReceiveEvent.remove();
        }
    }
}
