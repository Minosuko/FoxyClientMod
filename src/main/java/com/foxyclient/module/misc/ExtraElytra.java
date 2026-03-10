package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** ExtraElytra - Enhanced elytra control and boost (Wurst port). */
public class ExtraElytra extends Module {
    private final NumberSetting boostPower = addSetting(new NumberSetting("Boost", "Boost power", 1.5, 0.5, 5.0));

    public ExtraElytra() { super("ExtraElytra", "Enhanced elytra control", Category.MISC); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || !mc.player.isGliding()) return;
        if (mc.options.jumpKey.isPressed()) {
            mc.player.setVelocity(mc.player.getVelocity().add(
                mc.player.getRotationVec(1.0f).multiply(boostPower.get() * 0.1)
            ));
        }
    }
}
