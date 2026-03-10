package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.util.PlayerInput;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Various flight modes.
 */
public class Fly extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Flight mode", "Vanilla", "Vanilla", "Creative", "Packet"));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Flight speed", 2.0, 0.1, 10.0));

    public Fly() {
        super("Fly", "Allows you to fly", Category.MOVEMENT, GLFW.GLFW_KEY_F);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        if (mode.get().equals("Creative")) {
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().allowFlying = true;
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.getAbilities().flying = false;
        mc.player.getAbilities().allowFlying = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        switch (mode.get()) {
            case "Vanilla" -> {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed((float) (speed.get() / 10.0));
            }
            case "Creative" -> {
                mc.player.getAbilities().flying = true;
                mc.player.getAbilities().setFlySpeed((float) (speed.get() / 10.0));
            }
            case "Packet" -> {
                mc.player.setVelocity(0, 0, 0);
                double yaw = Math.toRadians(mc.player.getYaw());
                double forward = mc.player.input.playerInput.forward() ? 1 : (mc.player.input.playerInput.backward() ? -1 : 0);
                double sideways = mc.player.input.playerInput.left() ? 1 : (mc.player.input.playerInput.right() ? -1 : 0);
                double spd = speed.get() / 5.0;

                if (forward != 0 || sideways != 0) {
                    double moveAngle = Math.atan2(-sideways, forward) + yaw;
                    mc.player.setVelocity(
                        -Math.sin(moveAngle) * spd,
                        mc.player.getVelocity().y,
                        Math.cos(moveAngle) * spd
                    );
                }
                if (mc.options.jumpKey.isPressed()) mc.player.setVelocity(mc.player.getVelocity().add(0, spd, 0));
                if (mc.options.sneakKey.isPressed()) mc.player.setVelocity(mc.player.getVelocity().add(0, -spd, 0));
            }
        }
    }
}
