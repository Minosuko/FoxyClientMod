package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Automatically respawn when killed (clicks respawn button).
 */
public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Auto respawn on death", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;
        
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.DeathScreen) {
            mc.player.requestRespawn();
            mc.setScreen(null);
        } else if (mc.player.isDead()) {
            mc.player.requestRespawn();
        }
    }
}
