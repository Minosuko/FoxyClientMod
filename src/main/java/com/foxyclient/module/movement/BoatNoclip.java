package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
/** BoatNoclip - Noclip while in a boat. */
public class BoatNoclip extends Module {
    public BoatNoclip() { super("BoatNoclip", "Noclip in a boat", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck() || !mc.player.hasVehicle()) return;
        mc.player.getVehicle().noClip = true;
    }
    @Override public void onDisable() { if (mc.player != null && mc.player.hasVehicle()) mc.player.getVehicle().noClip = false; }
}
