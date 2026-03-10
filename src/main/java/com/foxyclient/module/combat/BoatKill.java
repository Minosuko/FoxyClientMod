package com.foxyclient.module.combat;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Hand;
/** BoatKill - Kills players in boats. */
public class BoatKill extends Module {
    public BoatKill() { super("BoatKill", "Kill players riding boats", Category.COMBAT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof BoatEntity boat && mc.player.distanceTo(boat) <= 4) {
                for (Entity passenger : boat.getPassengerList()) {
                    mc.interactionManager.attackEntity(mc.player, passenger);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }
}
