package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;

/** GamemodeNotifier - Notifies when gamemode changes (own or others). */
public class GamemodeNotifier extends Module {
    public GamemodeNotifier() { super("GamemodeNotifier", "Notify on gamemode changes", Category.MISC); }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof GameStateChangeS2CPacket pkt) {
            if (pkt.getReason() == GameStateChangeS2CPacket.GAME_MODE_CHANGED) {
                int mode = (int) pkt.getValue();
                String name = switch (mode) {
                    case 0 -> "Survival";
                    case 1 -> "Creative";
                    case 2 -> "Adventure";
                    case 3 -> "Spectator";
                    default -> "Unknown (" + mode + ")";
                };
                info("§eGamemode changed to §a" + name);
            }
        }
    }
}
