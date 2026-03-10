package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;

/**
 * XCarry - Store items in the 2x2 crafting grid slots as extra inventory.
 * Works by cancelling the close-inventory packet so the server doesn't clear the grid.
 */
public class XCarry extends Module {
    public XCarry() {
        super("XCarry", "Extra inventory slots (crafting grid)", Category.PLAYER);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;

        // Cancel the close-screen packet for the player's inventory (syncId 0)
        // This prevents the server from clearing the 2x2 crafting grid
        if (event.getPacket() instanceof CloseHandledScreenC2SPacket pkt) {
            if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
                event.cancel();
            }
        }
    }

    @Override
    public void onDisable() {
        // When disabling, send the close packet so crafting grid clears normally
        if (mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
        }
    }
}
