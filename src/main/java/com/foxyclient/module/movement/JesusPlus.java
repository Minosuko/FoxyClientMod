package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
/** JesusPlus - Walk on water and lava. */
public class JesusPlus extends Module {
    public JesusPlus() { super("JesusPlus", "Walk on water and lava", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.isTouchingWater() && !mc.player.isSneaking()) mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
        if (mc.player.isInLava() && !mc.player.isSneaking()) mc.player.setVelocity(mc.player.getVelocity().x, 0.1, mc.player.getVelocity().z);
    }
}
