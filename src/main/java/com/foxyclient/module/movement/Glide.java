package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** Glide - Slows fall speed to glide (Wurst port). */
public class Glide extends Module {
    private final NumberSetting fallSpeed = addSetting(new NumberSetting("FallSpeed", "Fall speed", 0.1, 0.01, 0.5));
    public Glide() { super("Glide", "Glide slowly through the air", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || mc.player.isOnGround()) return;
        if (mc.player.getVelocity().y < -fallSpeed.get()) {
            mc.player.setVelocity(mc.player.getVelocity().x, -fallSpeed.get(), mc.player.getVelocity().z);
        }
    }
}
