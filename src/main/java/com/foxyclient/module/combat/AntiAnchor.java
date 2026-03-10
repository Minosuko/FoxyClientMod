package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

/**
 * Prevents interacting with respawn anchors in the overworld.
 */
public class AntiAnchor extends Module {
    public AntiAnchor() {
        super("AntiAnchor", "Protects against anchor explosions", Category.COMBAT);
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
            if (!mc.world.getRegistryKey().getValue().getPath().equals("overworld")) return; // Anchors are for nether
            
            if (mc.world.getBlockState(packet.getBlockHitResult().getBlockPos()).getBlock() == Blocks.RESPAWN_ANCHOR) {
                event.cancel();
                info("Blocked accidental anchor interaction!");
            }
        }
    }
}
