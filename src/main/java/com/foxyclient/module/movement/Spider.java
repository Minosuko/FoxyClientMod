package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
/** Spider - Climb any wall like a spider. */
public class Spider extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Climb speed", 0.2, 0.1, 0.5));
    public Spider() { super("Spider", "Climb any wall", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.horizontalCollision) mc.player.setVelocity(mc.player.getVelocity().x, speed.get(), mc.player.getVelocity().z);
    }
}
