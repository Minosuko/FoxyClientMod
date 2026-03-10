package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

/**
 * Improves air control and maintains speed while turning.
 */
public class Strafe extends Module {
    public Strafe() {
        super("Strafe+", "Better air control and speed maintenance", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.isOnGround() || mc.player.isSneaking() || mc.player.isSwimming()) return;

        double forward = mc.player.input.playerInput.forward() ? 1 : (mc.player.input.playerInput.backward() ? -1 : 0);
        double side = mc.player.input.playerInput.left() ? 1 : (mc.player.input.playerInput.right() ? -1 : 0);
        float yaw = mc.player.getYaw();

        if (forward == 0 && side == 0) {
            mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
        } else {
            if (forward != 0) {
                if (side > 0) yaw += (forward > 0 ? -45 : 45);
                else if (side < 0) yaw += (forward > 0 ? 45 : -45);
                side = 0;
                if (forward > 0) forward = 1;
                else if (forward < 0) forward = -1;
            }
            
            double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
            double cos = Math.cos(Math.toRadians(yaw + 90));
            double sin = Math.sin(Math.toRadians(yaw + 90));
            
            mc.player.setVelocity(forward * speed * cos + side * speed * sin, mc.player.getVelocity().y, forward * speed * sin - side * speed * cos);
        }
    }
}
