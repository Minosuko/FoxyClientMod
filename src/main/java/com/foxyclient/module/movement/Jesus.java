package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;

/**
 * Walk on water (Jesus mode).
 */
public class Jesus extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Water walk mode", "Solid", "Solid", "Dolphin"));

    public Jesus() {
        super("Jesus", "Walk on water", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (mc.player.isTouchingWater()) {
            switch (mode.get()) {
                case "Solid" -> {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.11, mc.player.getVelocity().z);
                    if (mc.player.isSubmergedInWater()) {
                        mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
                    }
                }
                case "Dolphin" -> {
                    mc.player.setVelocity(
                        mc.player.getVelocity().x * 1.2,
                        mc.player.getVelocity().y + 0.03,
                        mc.player.getVelocity().z * 1.2
                    );
                }
            }
        }
    }
}
