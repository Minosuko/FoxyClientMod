package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.mixin.PlayerMoveC2SPacketAccessor;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * AntiHunger - Reduces hunger drain by spoofing movement packets.
 * Works by spoofing the on-ground flag to true in outgoing movement packets
 * and cancelling sprint action packets sent to the server.
 *
 * Server-side hunger mechanics:
 * - Sprinting causes 0.1 exhaustion per meter moved
 * - Being airborne (jumping) causes 0.05 exhaustion per jump
 * - By spoofing onGround=true and hiding sprint state, we reduce exhaustion
 */
public class AntiHunger extends Module {
    private final BoolSetting spoofGround = addSetting(new BoolSetting("SpoofGround", "Spoof on-ground flag to reduce jump exhaustion", true));
    private final BoolSetting noSprint = addSetting(new BoolSetting("NoSprint", "Cancel sprint packets to hide sprint state from server", true));

    public AntiHunger() {
        super("AntiHunger", "Reduces hunger loss", Category.PLAYER);
    }

    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        if (nullCheck()) return;

        // Spoof on-ground flag in all movement packet types
        if (spoofGround.get() && event.getPacket() instanceof PlayerMoveC2SPacket) {
            ((PlayerMoveC2SPacketAccessor) event.getPacket()).setOnGround(true);
        }

        // Cancel sprint start/stop packets so the server never knows we're sprinting
        if (noSprint.get() && event.getPacket() instanceof ClientCommandC2SPacket pkt) {
            if (pkt.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING
                    || pkt.getMode() == ClientCommandC2SPacket.Mode.STOP_SPRINTING) {
                event.cancel();
            }
        }
    }

    public boolean shouldModifyPackets() {
        return isEnabled();
    }
}
