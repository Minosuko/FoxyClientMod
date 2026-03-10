package com.foxyclient.event.events;

import com.foxyclient.event.Event;
import net.minecraft.network.packet.Packet;

/**
 * Fired when a packet is sent or received.
 */
public class PacketEvent extends Event {
    private Packet<?> packet;
    private final Direction direction;

    public enum Direction {
        SEND, RECEIVE
    }

    public PacketEvent(Packet<?> packet, Direction direction) {
        this.packet = packet;
        this.direction = direction;
    }

    public Packet<?> getPacket() { return packet; }
    public void setPacket(Packet<?> packet) { this.packet = packet; }
    public Direction getDirection() { return direction; }

    public static class Send extends PacketEvent {
        public Send(Packet<?> packet) { super(packet, Direction.SEND); }
    }

    public static class Receive extends PacketEvent {
        public Receive(Packet<?> packet) { super(packet, Direction.RECEIVE); }
    }
}
