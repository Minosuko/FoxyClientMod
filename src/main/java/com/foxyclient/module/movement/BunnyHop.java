package com.foxyclient.module.movement;


import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.KeySetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Automatically jumps and boosts speed while moving.
 * Mode: Standard - Basic speed boost.
 * Mode: MineHop - Source Engine style air acceleration (A/D strafing).
 */
public class BunnyHop extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "BHOP Mode", "Standard", "Standard", "MineHop"));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Standard Jump speed boost", 1.0, 0.1, 2.0));
    private final NumberSetting airAccelerate = addSetting(new NumberSetting("AirAccelerate", "MineHop Air Acceleration", 30.0, 1.0, 100.0));
    private final NumberSetting maxAirSpeed = addSetting(new NumberSetting("MaxAirSpeed", "MineHop Max air speed", 0.6, 0.1, 2.0));
    private final KeySetting minehopKey = addSetting(new KeySetting("Minehop Key", "Key to enable MineHop physics", GLFW.GLFW_KEY_SPACE));
    private final BoolSetting legacyMinehop = addSetting(new BoolSetting("BoostWhileMining", "Boost while mining", false));

    public BunnyHop() {
        super("BunnyHop", "Source Engine style Bunnyhopping", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        boolean isMineHopActive = mode.get().equals("MineHop") && GLFW.glfwGetKey(mc.getWindow().getHandle(), minehopKey.get()) == GLFW.GLFW_PRESS;
        
        float forward = mc.player.input.playerInput.forward() ? 1.0f : (mc.player.input.playerInput.backward() ? -1.0f : 0.0f);
        float side = mc.player.input.playerInput.left() ? 1.0f : (mc.player.input.playerInput.right() ? -1.0f : 0.0f);
        boolean isMoving = forward != 0 || side != 0;

        if (isMoving || isMineHopActive) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            } else {
                if (mode.get().equals("MineHop")) {
                    applyMineHopPhysics(forward, side);
                } else {
                    applyStandardPhysics();
                }
            }
        }

        // Legacy BoostWhileMining logic
        if (legacyMinehop.get() && mc.interactionManager.isBreakingBlock()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    private void applyStandardPhysics() {
        Vec3d velocity = mc.player.getVelocity();
        double currentSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (currentSpeed < speed.get() * 0.2873) {
            mc.player.setVelocity(velocity.x * 1.05, velocity.y, velocity.z * 1.05);
        }
    }

    private void applyMineHopPhysics(float forward, float side) {
        float yaw = mc.player.getYaw();

        // Calculate wish direction
        double wishX = side * Math.cos(Math.toRadians(yaw)) - forward * Math.sin(Math.toRadians(yaw));
        double wishZ = side * Math.sin(Math.toRadians(yaw)) + forward * Math.cos(Math.toRadians(yaw));
        
        if (wishX == 0 && wishZ == 0) return;
        
        // Normalize wish direction
        double mag = Math.sqrt(wishX * wishX + wishZ * wishZ);
        wishX /= mag;
        wishZ /= mag;

        Vec3d vel = mc.player.getVelocity();
        double currentSpeedInWishDir = vel.x * wishX + vel.z * wishZ;
        
        double addSpeed = maxAirSpeed.get() - currentSpeedInWishDir;
        if (addSpeed > 0) {
            double accelSpeed = airAccelerate.get() * 0.01 * maxAirSpeed.get(); // Scale acceleration
            if (accelSpeed > addSpeed) accelSpeed = addSpeed;
            
            mc.player.setVelocity(vel.x + wishX * accelSpeed, vel.y, vel.z + wishZ * accelSpeed);
        }
    }
}
