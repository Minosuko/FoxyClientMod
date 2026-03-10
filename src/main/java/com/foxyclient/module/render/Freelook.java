package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Freelook - Rotate camera independently from player body, like F5 but free.
 * Press keybind to activate, move mouse to look around, player body stays still.
 */
public class Freelook extends Module {
    private final NumberSetting sensitivity = addSetting(new NumberSetting("Sensitivity", "Look sensitivity", 1.0, 0.1, 3.0));
    private final BoolSetting f5Mode = addSetting(new BoolSetting("F5Mode", "Third person while active", true));

    private boolean active = false;
    private float savedYaw, savedPitch;
    private float cameraYaw, cameraPitch;
    private int savedPerspective;

    public Freelook() {
        super("Freelook", "Free camera rotation (F5 style)", Category.RENDER, GLFW.GLFW_KEY_V);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
        cameraYaw = savedYaw;
        cameraPitch = savedPitch;
        savedPerspective = mc.options.getPerspective().ordinal();
        if (f5Mode.get()) {
            mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
        }
        active = true;
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.player.setYaw(savedYaw);
        mc.player.setPitch(savedPitch);
        mc.options.setPerspective(net.minecraft.client.option.Perspective.values()[savedPerspective]);
        active = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!active) return;

        // Keep server-side rotation fixed
        // The camera rotation is handled by overriding the camera in the mixin
        // Player body maintains saved rotation for movement
    }

    public boolean isFreelookActive() { return active && isEnabled(); }
    public float getCameraYaw() { return cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }
    public void updateCamera(float deltaYaw, float deltaPitch) {
        cameraYaw += deltaYaw * sensitivity.get();
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch + deltaPitch * sensitivity.get().floatValue()));
    }
}
