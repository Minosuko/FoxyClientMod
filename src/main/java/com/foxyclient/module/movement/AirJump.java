package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Allows the player to jump in mid-air.
 */
public class AirJump extends Module {
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Jump height", 0.42, 0.1, 1.0));
    private final NumberSetting cooldown = addSetting(new NumberSetting("Cooldown", "Jump cooldown (ticks)", 2, 0, 20));
    
    private int tickCounter = 0;

    public AirJump() {
        super("AirJump", "Allows you to jump while in the air", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (tickCounter > 0) {
            tickCounter--;
        }

        if (mc.options.jumpKey.isPressed() && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isSubmergedInWater()) {
            if (tickCounter == 0) {
                Vec3d vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, height.get().doubleValue(), vel.z);
                tickCounter = cooldown.get().intValue();
            }
        }
    }
}
