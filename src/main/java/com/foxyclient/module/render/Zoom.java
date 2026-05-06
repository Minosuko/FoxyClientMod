package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.zoom.SmoothInterpolator;
import com.foxyclient.util.zoom.ZoomHelper;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    private final NumberSetting initialZoom = addSetting(
            new NumberSetting("Initial Zoom", "Starting zoom magnification", 4.0, 1.0, 20.0));
    private final NumberSetting zoomStep = addSetting(
            new NumberSetting("Zoom Step", "Magnification per scroll step (%)", 110.0, 101.0, 200.0));
    private final NumberSetting maxSteps = addSetting(
            new NumberSetting("Max Steps", "Maximum scroll zoom steps", 20.0, 0.0, 100.0));
    private final NumberSetting smoothness = addSetting(
            new NumberSetting("Smoothness", "Animation smoothness (0.0 = instant)", 0.3, 0.0, 1.0));
    private final BoolSetting smoothCamera = addSetting(
            new BoolSetting("Smooth Camera", "Cinematic mouse smoothing while zoomed", true));

    private final ZoomHelper zoomHelper;
    private int scrollSteps = 0;
    private boolean zooming = false;
    private boolean wasSmoothCamera;

    public Zoom() {
        super("Zoom", "Advanced camera zoom (Zoomify-style)", Category.RENDER, GLFW.GLFW_KEY_C);
        
        this.zoomHelper = new ZoomHelper(
                new SmoothInterpolator(smoothness::get),
                new SmoothInterpolator(smoothness::get),
                () -> initialZoom.get().intValue(),
                () -> zoomStep.get().intValue(),
                () -> maxSteps.get().intValue()
        );
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        zooming = true;
        wasSmoothCamera = mc.options.smoothCameraEnabled;
        if (smoothCamera.get()) {
            mc.options.smoothCameraEnabled = true;
        }
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        mc.options.smoothCameraEnabled = wasSmoothCamera;
        zooming = false;
        scrollSteps = 0;
        zoomHelper.reset();
    }

    @com.foxyclient.event.EventHandler
    public void onTick(com.foxyclient.event.events.TickEvent event) {
        if (nullCheck()) return;

        // FoxyMoonlightClient polling approach
        boolean keyDown = GLFW.glfwGetKey(mc.getWindow().getHandle(), getKeybind()) == GLFW.GLFW_PRESS;
        
        if (zooming != keyDown) {
            if (keyDown) {
                if (smoothCamera.get()) mc.options.smoothCameraEnabled = true;
            } else {
                mc.options.smoothCameraEnabled = wasSmoothCamera;
            }
        }
        
        zooming = keyDown;

        if (!zooming) {
            scrollSteps = 0;
            zoomHelper.reset();
        }

        zoomHelper.tick(zooming, scrollSteps, 0.05); // 0.05s per tick (20tps)

        // If animation finished and we released key, disable module to stop ticking
        if (!zooming && zoomHelper.getZoomDivisor(1.0f) <= 1.001) {
            setEnabled(false);
        }
    }

    public void onScroll(double delta) {
        if (zooming) {
            if (delta > 0) scrollSteps++;
            else if (delta < 0) scrollSteps--;
            
            scrollSteps = Math.max(0, Math.min(scrollSteps, maxSteps.get().intValue()));
        }
    }

    public boolean isZooming() {
        return isEnabled() && (zooming || zoomHelper.getZoomDivisor(1.0f) > 1.001);
    }

    public double getFovMultiplier(float tickDelta) {
        if (!isEnabled()) return 1.0;
        return 1.0 / zoomHelper.getZoomDivisor(tickDelta);
    }

    public static Zoom get() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(Zoom.class);
    }
}
