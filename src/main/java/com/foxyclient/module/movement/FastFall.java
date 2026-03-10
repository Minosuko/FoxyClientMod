package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Increases downward velocity when falling.
 */
public class FastFall extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fall speed multiplier", 2.0, 1.0, 5.0));

    public FastFall() {
        super("FastFall", "Falls faster when in air", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.onGround || mc.player.isGliding() || mc.player.getAbilities().flying) return;

        if (mc.player.getVelocity().y < 0) {
            mc.player.setVelocity(mc.player.getVelocity().add(0, -speed.get() * 0.1, 0));
        }
    }
}
