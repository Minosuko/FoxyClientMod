package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook module — fully ported from FoxyMoonlightClient.
 * Allows independent camera rotation while maintaining player orientation.
 * Supports Hold and Toggle activation modes, interpolated rotation, and FOV easing.
 */
public class Freelook extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Activation mode", "Hold", "Hold", "Toggle"));
    private final NumberSetting sensitivity = addSetting(new NumberSetting("Sensitivity", "Look sensitivity", 1.0, 0.1, 3.0));
    private final BoolSetting f5Mode = addSetting(new BoolSetting("F5Mode", "Third person while active", true));

    private float cameraYaw, cameraPitch;
    private float prevCameraYaw, prevCameraPitch;
    private int savedPerspective;
    private boolean active = false;
    private boolean lastKeyState = false;

    // Ported easing and interpolation from FoxyMoonlight version
    private float progress = 0;
    private float prevProgress = 0;

    public Freelook() {
        super("Freelook", "Independent camera rotation", Category.RENDER, GLFW.GLFW_KEY_V);
    }

    @com.foxyclient.event.EventHandler
    public void onTick(com.foxyclient.event.events.TickEvent event) {
        if (mc.player == null) return;

        boolean keyDown = GLFW.glfwGetKey(mc.getWindow().getHandle(), getKeybind()) == GLFW.GLFW_PRESS;
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        prevProgress = progress;

        boolean targetState = active;
        if (mode.is("Hold")) {
            targetState = keyDown && isEnabled();
        } else if (isEnabled()) {
            if (keyDown && !lastKeyState) {
                targetState = !active;
            }
        } else {
            targetState = false;
        }

        lastKeyState = keyDown;

        if (targetState) {
            if (!active) {
                active = true;
                cameraYaw = mc.player.getYaw();
                cameraPitch = mc.player.getPitch();
                prevCameraYaw = cameraYaw;
                prevCameraPitch = cameraPitch;
                savedPerspective = mc.options.getPerspective().ordinal();
                if (f5Mode.get()) {
                    mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                }
            }
            if (progress < 1.0f) progress = Math.min(1.0f, progress + 0.15f);
        } else {
            if (active) {
                active = false;
                mc.options.setPerspective(Perspective.values()[savedPerspective]);
            }
            if (progress > 0.0f) progress = Math.max(0.0f, progress - 0.15f);
        }
    }

    public void onMouseUpdate(double dx, double dy) {
        if (!isActive()) return;

        // Ported sensitivity logic from FoxyMoonlight (delta * sensitivity * 0.5)
        cameraYaw += (float) (dx * sensitivity.get() * 0.5);
        cameraPitch += (float) (dy * sensitivity.get() * 0.5);
        cameraPitch = MathHelper.clamp(cameraPitch, -90f, 90f);
    }

    public boolean isActive() {
        return active && isEnabled();
    }

    public float getCameraYaw() { return cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }

    public float getInterpolatedYaw(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevCameraYaw, cameraYaw);
    }

    public float getInterpolatedPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevCameraPitch, cameraPitch);
    }

    public double getFovMultiplier(float tickDelta) {
        float fovProgress = MathHelper.lerp(tickDelta, prevProgress, progress);
        // Quartic out easing
        float eased = 1.0f - (float) Math.pow(1.0f - fovProgress, 4);
        return 1.0 + 0.2 * eased; // 1.2x zoom out
    }

    @Override
    public void onDisable() {
        if (active) {
            if (mc.player != null) {
                mc.options.setPerspective(Perspective.values()[savedPerspective]);
            }
        }
        active = false;
        progress = 0;
        prevProgress = 0;
    }

    public static Freelook get() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(Freelook.class);
    }
}
