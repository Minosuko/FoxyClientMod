package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/** BoatPhase - Phases through blocks while in a boat. */
public class BoatPhase extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Phase speed", 1.0, 0.1, 3.0));
    public BoatPhase() { super("BoatPhase", "Phase through blocks in a boat", Category.MOVEMENT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || !mc.player.hasVehicle()) return;
        var vehicle = mc.player.getVehicle();
        vehicle.noClip = true;
        vehicle.setVelocity(mc.player.getRotationVec(1.0f).multiply(speed.get()));
    }

    @Override public void onDisable() {
        if (mc.player != null && mc.player.hasVehicle()) mc.player.getVehicle().noClip = false;
    }
}
