package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;

/**
 * Fly while in a boat.
 */
public class BoatFly extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Flight speed", 2.0, 1, 50.0));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "BoatFly mode", "Normal", "Normal", "Packet"));

    public BoatFly() {
        super("BoatFly", "Fly while riding a boat", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof BoatEntity boat)) return;

        boat.setVelocity(0, 0, 0);

        double yaw = Math.toRadians(mc.player.getYaw());
        double spd = speed.get() * 0.05;

        if (mc.options.forwardKey.isPressed()) {
            boat.setVelocity(boat.getVelocity().add(-Math.sin(yaw) * spd, 0, Math.cos(yaw) * spd));
        }
        if (mc.options.backKey.isPressed()) {
            boat.setVelocity(boat.getVelocity().add(Math.sin(yaw) * spd, 0, -Math.cos(yaw) * spd));
        }
        if (mc.options.jumpKey.isPressed()) {
            boat.setVelocity(boat.getVelocity().add(0, spd * 0.5, 0));
        }
        if (mc.options.sneakKey.isPressed()) {
            boat.setVelocity(boat.getVelocity().add(0, -spd * 0.5, 0));
        }
    }
}
