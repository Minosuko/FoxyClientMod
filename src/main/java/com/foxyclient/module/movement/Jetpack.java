package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** Jetpack - Propels player upward repeatedly like a jetpack. */
public class Jetpack extends Module {
    private final NumberSetting power = addSetting(new NumberSetting("Power", "Jetpack power", 0.5, 0.1, 2.0));
    public Jetpack() { super("Jetpack", "Fly like a jetpack", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().add(0, power.get() * 0.15, 0));
        }
    }
}
