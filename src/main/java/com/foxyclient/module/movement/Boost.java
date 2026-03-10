package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** Boost - Speed boost on use (Cornos port). */
public class Boost extends Module {
    private final NumberSetting multiplier = addSetting(new NumberSetting("Multiplier", "Speed multiplier", 2.0, 1.1, 5.0));
    public Boost() { super("Boost", "Speed boost", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        mc.player.setVelocity(mc.player.getVelocity().multiply(multiplier.get(), 1, multiplier.get()));
    }
}
