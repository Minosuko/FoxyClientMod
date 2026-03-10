package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Faster downward movement when stepping off edges.
 */
public class ReverseStep extends Module {
    private final NumberSetting height = addSetting(new NumberSetting("Height", "Max reverse step height", 2.0, 0.5, 4.0));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fall speed", 1.0, 0.1, 3.0));

    public ReverseStep() {
        super("ReverseStep", "Faster downward movement", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!mc.player.isOnGround()) return;
        if (mc.player.isSneaking()) return;

        // Check if there's air below within height range
        for (int i = 1; i <= height.get(); i++) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().down(i)).isAir()) return;
        }

        mc.player.setVelocity(mc.player.getVelocity().x, -speed.get(), mc.player.getVelocity().z);
    }
}
