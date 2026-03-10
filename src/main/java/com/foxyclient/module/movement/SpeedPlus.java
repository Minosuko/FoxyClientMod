package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
/** SpeedPlus - Enhanced speed with bypass modes. */
public class SpeedPlus extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Speed multiplier", 1.5, 1.1, 5.0));
    public SpeedPlus() { super("SpeedPlus", "Enhanced speed hack", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck() || !mc.player.isOnGround()) return;
        mc.player.setVelocity(mc.player.getVelocity().multiply(speed.get(), 1, speed.get()));
    }
}
