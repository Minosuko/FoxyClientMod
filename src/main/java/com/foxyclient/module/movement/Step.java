package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Allows stepping up blocks without jumping.
 */
public class Step extends Module {
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Step height", 1.0, 0.5, 2.5));

    public Step() {
        super("Step", "Step up blocks without jumping", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        // mc.player.setStepHeight(height.get().floatValue());
    }

    @Override
    public void onDisable() {
        // if (!nullCheck()) mc.player.setStepHeight(0.6f);
    }
}
