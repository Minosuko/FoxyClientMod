package com.foxyclient.module.render;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.KeyEvent;
import com.foxyclient.event.events.MouseScrollEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Zoom functionality like OptiFine zoom.
 */
public class Zoom extends Module {
    private final NumberSetting zoomLevel = addSetting(new NumberSetting("Level", "Zoom magnification", 4.0, 1.0, 100.0));
    private final ModeSetting easing = addSetting(new ModeSetting("Smoothing", "Easing function style", "Ease in-out Cubic",
            "Ease in-out Cubic", "Ease in Cubic", "Ease out Cubic",
            "Ease in-out Quad", "Ease in Quad", "Ease out Quad",
            "Ease in-out Sine", "Ease in Sine", "Ease out Sine",
            "Ease in-out Exp", "Ease in Exp", "Ease out Exp",
            "linear", "instant"
    ));
    private final NumberSetting duration = addSetting(new NumberSetting("Duration", "Transition time in ms", 200.0, 0.0, 1000.0));
    private final BoolSetting smoothCamera = addSetting(new BoolSetting("Smooth Camera", "Cinematic mouse smoothing", true));
    private final NumberSetting scrollSpeed = addSetting(new NumberSetting("Scroll Speed", "Speed of scroll magnification", 1.0, 0.1, 5.0));

    private double animProgress = 0.0;
    private double smoothedZoomLevel = 4.0;
    private long lastFrameTimeNano = 0;
    private boolean releasing = false;
    private boolean wasSmoothCamera;

    public Zoom() {
        super("Zoom", "OptiFine-style zoom", Category.RENDER, GLFW.GLFW_KEY_C);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        releasing = false;
        lastFrameTimeNano = System.nanoTime();
        
        wasSmoothCamera = mc.options.smoothCameraEnabled;
        if (smoothCamera.get()) {
            mc.options.smoothCameraEnabled = true;
        }

        if (easing.is("instant")) {
            animProgress = 1.0;
        }
        smoothedZoomLevel = zoomLevel.get();
    }

    @Override
    public void onDisable() {
        if (nullCheck()) return;
        if (smoothCamera.get()) {
            mc.options.smoothCameraEnabled = wasSmoothCamera;
        }
        animProgress = 0.0;
        releasing = false;
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        long now = System.nanoTime();
        if (lastFrameTimeNano == 0) {
            lastFrameTimeNano = now;
            return;
        }
        double deltaMs = (now - lastFrameTimeNano) / 1_000_000.0;
        lastFrameTimeNano = now;

        if (!easing.is("instant")) {
            // 1. Smooth the activation/deactivation
            double dur = duration.get();
            if (dur <= 0) {
                animProgress = releasing ? 0.0 : 1.0;
                if (releasing) {
                    setEnabled(false);
                    return;
                }
            } else {
                double step = deltaMs / dur;
                if (releasing) {
                    animProgress -= step;
                    if (animProgress <= 0.0) {
                        animProgress = 0.0;
                        setEnabled(false);
                        return;
                    }
                } else {
                    animProgress += step;
                    if (animProgress > 1.0) animProgress = 1.0;
                }
            }

            // 2. Smooth the scroll magnification (liquid feel)
            // Using frame-rate independent lerp: 1 - exp(-speed * dt)
            double lerpFactor = 1.0 - Math.exp(-0.01 * deltaMs);
            smoothedZoomLevel += (zoomLevel.get() - smoothedZoomLevel) * lerpFactor;
        } else {
            animProgress = releasing ? 0.0 : 1.0;
            smoothedZoomLevel = zoomLevel.get();
            if (releasing) setEnabled(false);
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (!isEnabled()) return;
        
        if (event.getKey() == getKeybind() && event.getAction() == 0) {
            releasing = true;
        }
    }

    @EventHandler
    public void onMouseScroll(MouseScrollEvent event) {
        if (!isEnabled() || releasing) return;

        double deltaVertical = event.getVertical() * scrollSpeed.get();
        double current = zoomLevel.get();
        
        double next;
        if (deltaVertical > 0) {
            next = current * 1.15;
        } else if (deltaVertical < 0) {
            next = current / 1.15;
        } else {
            return;
        }

        if (next < 1.0) next = 1.0;
        if (next > 100.0) next = 100.0;

        zoomLevel.set(next);
        event.cancel();
    }

    public double getMagnification() {
        if (animProgress <= 0.0) return 1.0;
        
        return 1.0 + (smoothedZoomLevel - 1.0) * getEasedProgress(animProgress);
    }

    private double getEasedProgress(double t) {
        if (t <= 0.0) return 0.0;
        if (t >= 1.0) return 1.0;

        String mode = easing.get();
        switch (mode) {
            case "Ease in Cubic": return t * t * t;
            case "Ease out Cubic": return 1.0 - Math.pow(1.0 - t, 3.0);
            case "Ease in-out Cubic": return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3.0) / 2.0;
            
            case "Ease in Quad": return t * t;
            case "Ease out Quad": return 1.0 - (1.0 - t) * (1.0 - t);
            case "Ease in-out Quad": return t < 0.5 ? 2.0 * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
            
            case "Ease in Sine": return 1.0 - Math.cos((t * Math.PI) / 2.0);
            case "Ease out Sine": return Math.sin((t * Math.PI) / 2.0);
            case "Ease in-out Sine": return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
            
            case "Ease in Exp": return Math.pow(2.0, 10.0 * t - 10.0);
            case "Ease out Exp": return 1.0 - Math.pow(2.0, -10.0 * t);
            case "Ease in-out Exp": return t < 0.5 ? Math.pow(2.0, 20.0 * t - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
            
            case "linear": return t;
            case "instant": return 1.0;
            default: return t;
        }
    }

    public static Zoom get() {
        return FoxyClient.INSTANCE.getModuleManager().getModule(Zoom.class);
    }
}
