package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Hand;

/** VehicleOneHit - Instantly destroys vehicles with one hit. */
public class VehicleOneHit extends Module {
    public VehicleOneHit() { super("VehicleOneHit", "One-hit destroy vehicles", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (mc.player.distanceTo(e) > 4.5) continue;
            if (e instanceof BoatEntity || e instanceof AbstractMinecartEntity) {
                for (int i = 0; i < 20; i++) {
                    mc.interactionManager.attackEntity(mc.player, e);
                }
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
