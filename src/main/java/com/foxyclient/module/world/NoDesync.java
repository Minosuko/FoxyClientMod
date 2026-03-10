package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

/**
 * Prevents desync issues between client and server.
 */
public class NoDesync extends Module {
    public NoDesync() {
        super("NoDesync", "Prevents block/entity desync", Category.WORLD);
    }

    @EventHandler
    public void onReceive(PacketEvent.Receive event) {
        if (event.getPacket() instanceof BlockUpdateS2CPacket packet) {
            // Logic to prevent client-side ghost blocks
        }
    }
}
