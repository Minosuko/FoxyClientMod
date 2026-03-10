package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** KnockbackPlus - Adds extra knockback to attacks via sprint resetting. */
public class KnockbackPlus extends Module {
    private final NumberSetting strength = addSetting(new NumberSetting("Strength", "Extra KB multiplier", 1.5, 1.0, 5.0));

    public KnockbackPlus() { super("KnockbackPlus", "Extra knockback on attacks", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.handSwinging && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            mc.player.setSprinting(true);
        }
    }
}
