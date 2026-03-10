package com.foxyclient.util;

import net.minecraft.client.MinecraftClient;

public class RotationManager {
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        EXTREME
    }

    private static float serverYaw;
    private static float serverPitch;
    private static float prevServerYaw;
    private static float prevServerPitch;
    private static boolean active;
    private static boolean normalize;
    private static long lastUpdateTick = -1;
    private static Priority currentPriority = Priority.LOW;

    public static void setRotation(float yaw, float pitch, boolean shouldNormalize, Priority priority) {
        if (active && priority.ordinal() < currentPriority.ordinal()) {
            return;
        }

        if (!active) {
            // Smoothly transition from current player rotation
            float playerYaw = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getYaw() : yaw;
            float playerPitch = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getPitch() : pitch;
            
            prevServerYaw = yaw + net.minecraft.util.math.MathHelper.wrapDegrees(playerYaw - yaw);
            prevServerPitch = playerPitch;
        } else {
            // Continually update prev values from last server values for smooth interpolation
            // Fix for smooth 360 rotation: Ensure prevYaw is within 180 degrees of yaw
            // This prevents linear interpolation from going "the long way" around the circle
            prevServerYaw = yaw + net.minecraft.util.math.MathHelper.wrapDegrees(serverYaw - yaw);
            prevServerPitch = serverPitch;
        }
        
        serverYaw = yaw;
        serverPitch = pitch;
        normalize = shouldNormalize;
        active = true;
        currentPriority = priority;
        lastUpdateTick = MinecraftClient.getInstance().world.getTime();
    }

    // Overload for backward compatibility/simpler use
    public static void setRotation(float yaw, float pitch, boolean shouldNormalize) {
        setRotation(yaw, pitch, shouldNormalize, Priority.MEDIUM);
    }

    public static float getInterpolatedYaw(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerpAngleDegrees(tickDelta, prevServerYaw, serverYaw);
    }

    public static float getInterpolatedPitch(float tickDelta) {
        return net.minecraft.util.math.MathHelper.lerp(tickDelta, prevServerPitch, serverPitch);
    }

    public static void update() {
        if (active && MinecraftClient.getInstance().world != null && MinecraftClient.getInstance().world.getTime() > lastUpdateTick) {
            active = false;
            currentPriority = Priority.LOW;
        }
    }

    public static float getServerYaw() { return serverYaw; }
    public static float getServerPitch() { return serverPitch; }
    public static float getPrevServerYaw() { return prevServerYaw; }
    public static float getPrevServerPitch() { return prevServerPitch; }
    public static boolean isActive() { return active; }
    public static boolean shouldNormalize() { return normalize; }
}
