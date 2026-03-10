package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.block.BedBlock;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

/**
 * Prevents interacting with beds in the overworld to avoid accidental explosions.
 */
public class AntiBed extends Module {
    public AntiBed() {
        super("AntiBed", "Prevents accidental bed explosions", Category.COMBAT);
    }

    @EventHandler
    public void onSendPacket(PacketEvent.Send event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof PlayerInteractBlockC2SPacket packet) {
            if (mc.world.getRegistryKey().getValue().getPath().equals("overworld")) return; // Beds are safe in overworld
            
            if (mc.world.getBlockState(packet.getBlockHitResult().getBlockPos()).getBlock() instanceof BedBlock) {
                event.cancel();
                info("Blocked accidental bed interaction!");
            }
        }
    }
}
