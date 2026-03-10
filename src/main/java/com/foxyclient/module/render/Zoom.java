package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Zoom functionality like OptiFine zoom.
 */
public class Zoom extends Module {
    private final NumberSetting zoomWorld = addSetting(new NumberSetting("Level", "Zoom magnification", 4.0, 1.5, 20.0));

    private double originalFov;
    private boolean wasSmoothCamera;

    public Zoom() {
        super("Zoom", "OptiFine-style zoom", Category.RENDER, GLFW.GLFW_KEY_C);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        originalFov = mc.options.getFov().getValue();
        wasSmoothCamera = mc.options.smoothCameraEnabled;
        mc.options.smoothCameraEnabled = true;
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.options.getFov().setValue((int) originalFov);
        mc.options.smoothCameraEnabled = wasSmoothCamera;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (originalFov < 30) originalFov = 70; // Fallback
        int targetFov = (int) (originalFov / zoomWorld.get());
        if (targetFov < 1) targetFov = 1; // Clamp but still might be illegal for option
        // To be safe and avoid the log error, if it's below 30, we just set to 30 or use a smaller magnification
        if (targetFov < 30) targetFov = 30; 
        mc.options.getFov().setValue(targetFov);
    }

    public double getZoomLevel() { return zoomWorld.get(); }
}
