package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
/** NoSlowPlus - Prevents all forms of slowdown including soul sand, honey, cobweb. */
public class NoSlowPlus extends Module {
    public NoSlowPlus() { super("NoSlowPlus", "No slowdown from anything", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        // Reset movement multipliers
        mc.player.setMovementSpeed(0.1f);
    }
}
