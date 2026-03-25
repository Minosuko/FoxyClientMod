package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;

/**
 * Increases movement speed.
 */
public class Speed extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Speed mode", "Vanilla", "Vanilla", "Strafe", "BHop", "Plus"));
    private final NumberSetting speedMultiplier = addSetting(new NumberSetting("Speed", "Speed multiplier", 1.5, 0.5, 5.0));

    public Speed() {
        super("Speed", "Move faster", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || mc.player.isGliding()) return;
        if (!mc.player.isOnGround() && mode.get().equals("Vanilla")) return;

        double yaw = Math.toRadians(mc.player.getYaw());
        double forward = mc.player.input.playerInput.forward() ? 1 : (mc.player.input.playerInput.backward() ? -1 : 0);
        double sideways = mc.player.input.playerInput.left() ? 1 : (mc.player.input.playerInput.right() ? -1 : 0);
        double spd = 0.2873 * speedMultiplier.get();

        switch (mode.get()) {
            case "Vanilla" -> {
                if (forward != 0 || sideways != 0) {
                    double moveAngle = Math.atan2(-sideways, forward) + yaw;
                    mc.player.setVelocity(
                        -Math.sin(moveAngle) * spd,
                        mc.player.getVelocity().y,
                        Math.cos(moveAngle) * spd
                    );
                }
            }
            case "Strafe" -> {
                if (forward != 0 || sideways != 0) {
                    double moveAngle = Math.atan2(-sideways, forward) + yaw;
                    mc.player.setVelocity(
                        -Math.sin(moveAngle) * spd,
                        mc.player.getVelocity().y,
                        Math.cos(moveAngle) * spd
                    );
                }
            }
            case "BHop" -> {
                if (forward != 0 || sideways != 0) {
                    if (mc.player.isOnGround()) {
                        mc.player.jump();
                    }
                    double moveAngle = Math.atan2(-sideways, forward) + yaw;
                    mc.player.setVelocity(
                        -Math.sin(moveAngle) * spd,
                        mc.player.getVelocity().y,
                        Math.cos(moveAngle) * spd
                    );
                }
            }
            case "Plus" -> {
                if (mc.player.isOnGround()) {
                    mc.player.setVelocity(mc.player.getVelocity().multiply(speedMultiplier.get(), 1, speedMultiplier.get()));
                }
            }
        }
    }
}
