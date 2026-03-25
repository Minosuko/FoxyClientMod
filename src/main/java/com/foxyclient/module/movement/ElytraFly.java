package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

/**
 * Enhanced Elytra flight control.
 */
public class ElytraFly extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Flight mode", "Plus", "Control", "Boost", "Packet", "Plus"));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Flight speed", 2.0, 0.1, 10.0));
    private final BoolSetting autoStart = addSetting(new BoolSetting("AutoStart", "Auto deploy elytra", true));
    private final BoolSetting noDurability = addSetting(new BoolSetting("NoDurability", "Reduce durability usage", false));

    public ElytraFly() {
        super("ElytraFly", "Enhanced Elytra control", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!mc.player.isGliding()) {
            if (autoStart.get() && !mc.player.onGround && mc.player.getVelocity().y < 0) {
                // Auto deploy elytra by pressing jump in mid-air
                mc.player.startGliding();
            }
            return;
        }

        double yaw = Math.toRadians(mc.player.getYaw());
        double pitch = Math.toRadians(mc.player.getPitch());
        double spd = speed.get();

        switch (mode.get()) {
            case "Control" -> {
                Vec3d vel = mc.player.getVelocity();
                double forward = mc.player.input.playerInput.forward() ? 1 : (mc.player.input.playerInput.backward() ? -1 : 0);
                double up = mc.options.jumpKey.isPressed() ? 1 : mc.options.sneakKey.isPressed() ? -1 : 0;

                if (forward > 0) {
                    double dx = -Math.sin(yaw) * Math.cos(pitch) * spd * 0.05;
                    double dy = -Math.sin(pitch) * spd * 0.05;
                    double dz = Math.cos(yaw) * Math.cos(pitch) * spd * 0.05;
                    mc.player.setVelocity(dx, dy + up * 0.05, dz);
                } else {
                    mc.player.setVelocity(vel.x * 0.99, up * 0.05, vel.z * 0.99);
                }
            }
            case "Boost" -> {
                if (mc.player.input.playerInput.forward()) {
                    Vec3d dir = mc.player.getRotationVector().multiply(spd * 0.1);
                    mc.player.addVelocity(dir.x, dir.y, dir.z);
                }
            }
            case "Packet" -> {
                mc.player.setVelocity(Vec3d.ZERO);
                double dx = -Math.sin(yaw) * spd * 0.05;
                double dz = Math.cos(yaw) * spd * 0.05;
                double dy = mc.options.jumpKey.isPressed() ? spd * 0.03 : mc.options.sneakKey.isPressed() ? -spd * 0.03 : 0;
                mc.player.setVelocity(dx, dy, dz);
            }
            case "Plus" -> {
                mc.player.setVelocity(mc.player.getRotationVec(1.0f).multiply(spd));
            }
        }
    }
}
