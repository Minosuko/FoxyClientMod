package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;

/**
 * Makes every hit a critical hit by adding small jumps.
 */
public class Criticals extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Critical method", "Packet", "Packet", "MiniJump"));

    public Criticals() {
        super("Criticals", "All hits become critical hits", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        // Criticals are applied via pre-attack hook, the tick handler is for MiniJump mode
        if (mode.get().equals("MiniJump") && mc.player.onGround) {
            // Small jump before attack to ensure critical
            // The actual jump is applied when attacking in combination with KillAura
        }
    }

    public boolean shouldCrit() {
        if (!isEnabled() || nullCheck()) return false;
        return mc.player.onGround;
    }

    public String getMode() {
        return mode.get();
    }
}
