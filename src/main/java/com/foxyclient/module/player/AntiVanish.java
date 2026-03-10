package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/** AntiVanish - Detects vanished players by tracking tab list changes. */
public class AntiVanish extends Module {
    public AntiVanish() { super("AntiVanish", "Detect vanished players", Category.PLAYER); }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (event.getPacket() instanceof net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket pkt) {
            for (var uuid : pkt.profileIds()) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
                if (entry != null) {
                    info("§e" + entry.getProfile().name() + " §7may have vanished!");
                }
            }
        }
    }
}
