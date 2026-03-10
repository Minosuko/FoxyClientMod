package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
/** ElytraFlyPlus - Enhanced elytra flight with boost and control. */
public class ElytraFlyPlus extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Flight speed", 2.0, 0.5, 10.0));
    public ElytraFlyPlus() { super("ElytraFlyPlus", "Enhanced elytra flight", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck() || !mc.player.isGliding()) return;
        mc.player.setVelocity(mc.player.getRotationVec(1.0f).multiply(speed.get()));
    }
}
