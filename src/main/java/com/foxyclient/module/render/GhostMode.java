package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/** GhostMode - Makes player invisible to self rendering (client-side). */
public class GhostMode extends Module {
    public GhostMode() { super("GhostMode", "Hide your own player model", Category.RENDER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        mc.player.setInvisible(true);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) mc.player.setInvisible(false);
    }
}
