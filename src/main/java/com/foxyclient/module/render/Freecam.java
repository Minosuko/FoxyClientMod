package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.util.math.Vec3d;

/**
 * Free camera mode - float around without moving player.
 */
public class Freecam extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Camera speed", 1.0, 0.1, 5.0));

    private Vec3d cameraPos;
    private float cameraYaw, cameraPitch;
    private Vec3d originalPos;

    public Freecam() {
        super("Freecam", "Detach camera from player", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        originalPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        cameraPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.setPosition(originalPos);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        cameraYaw = mc.player.getYaw();
        cameraPitch = mc.player.getPitch();

        double yaw = Math.toRadians(cameraYaw);
        double spd = speed.get();
        double dx = 0, dy = 0, dz = 0;

        if (mc.options.forwardKey.isPressed()) {
            dx -= Math.sin(yaw) * spd;
            dz += Math.cos(yaw) * spd;
        }
        if (mc.options.backKey.isPressed()) {
            dx += Math.sin(yaw) * spd;
            dz -= Math.cos(yaw) * spd;
        }
        if (mc.options.leftKey.isPressed()) {
            dx += Math.cos(yaw) * spd;
            dz += Math.sin(yaw) * spd;
        }
        if (mc.options.rightKey.isPressed()) {
            dx -= Math.cos(yaw) * spd;
            dz -= Math.sin(yaw) * spd;
        }
        if (mc.options.jumpKey.isPressed()) dy += spd;
        if (mc.options.sneakKey.isPressed()) dy -= spd;

        cameraPos = cameraPos.add(dx * 0.1, dy * 0.1, dz * 0.1);
        mc.player.setPosition(cameraPos);
        mc.player.setVelocity(Vec3d.ZERO);
    }

    public Vec3d getCameraPos() { return cameraPos; }
}
