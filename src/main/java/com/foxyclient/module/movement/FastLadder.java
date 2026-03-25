package com.foxyclient.module.movement;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
/** FastLadder - Climb ladders faster. */
public class FastLadder extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Climb speed", 0.3, 0.1, 1.0));
    public FastLadder() { super("FastLadder", "Climb ladders faster", Category.MOVEMENT); }
    @EventHandler public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.isClimbing()) mc.player.setVelocity(mc.player.getVelocity().x, speed.get(), mc.player.getVelocity().z);
    }
}
