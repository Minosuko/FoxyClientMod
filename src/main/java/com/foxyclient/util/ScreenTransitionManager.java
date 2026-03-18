package com.foxyclient.util;

import com.foxyclient.util.FoxyConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

public class ScreenTransitionManager {
    public static final ScreenTransitionManager INSTANCE = new ScreenTransitionManager();

    private float progress = 1.0f;
    private boolean transitioning = false;
    public boolean isCompletingTransition = false;
    private boolean isExiting = false;
    private boolean matrixPushedThisFrame = false;
    private Screen pendingScreen = null;

    private static final float TRANSITION_TIME_NS = 400_000_000.0f; // 400ms in nanoseconds
    private long lastUpdateTime = 0;

    public void startTransition(Screen nextScreen) {
        if (!FoxyConfig.INSTANCE.transitionsEnabled.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) mc.setScreen(nextScreen);
            return;
        }
        this.pendingScreen = nextScreen;
        this.transitioning = true;
        this.isExiting = true;
        this.progress = 0.0f;
        this.lastUpdateTime = System.nanoTime();
    }

    public void startEntryTransition() {
        if (!FoxyConfig.INSTANCE.transitionsEnabled.get()) return;

        pendingScreen = null;
        transitioning = true;
        isExiting = false;
        progress = 0.0f;
        lastUpdateTime = System.nanoTime();
    }

    public void update(float ignored) {
        if (!transitioning) return;

        long now = System.nanoTime();
        float delta = (now - lastUpdateTime) / TRANSITION_TIME_NS;
        lastUpdateTime = now;

        progress += delta;

        if (isExiting && progress >= 1.0f) {
            // End of Phase 1 (Exit): Switch screens
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                Screen target = pendingScreen;
                pendingScreen = null;
                
                if (target == null) {
                    // Closing to game — no Phase 2 needed
                    progress = 1.0f;
                    transitioning = false;
                    isExiting = false;
                    isCompletingTransition = true;
                    try {
                        mc.setScreen(null);
                    } finally {
                        isCompletingTransition = false;
                    }
                } else {
                    // Switching to another screen — start Phase 2 (Entry)
                    isExiting = false;
                    progress = 0.0f;
                    lastUpdateTime = System.nanoTime();
                    isCompletingTransition = true;
                    try {
                        mc.setScreen(target);
                    } finally {
                        isCompletingTransition = false;
                    }
                }
            }
        } else if (!isExiting && progress >= 1.0f) {
            // End of Phase 2 (Entry): Done
            progress = 1.0f;
            transitioning = false;
        }
    }

    public void applyTransition(DrawContext context) {
        if (!transitioning) return;

        matrixPushedThisFrame = true;
        float t = MathHelper.clamp(progress, 0.0f, 1.0f);
        float scale;
        float alpha;

        if (isExiting) {
            // Phase 1: Zoom In (1.0 -> 1.08) + Fade Out
            float ease = easeInQuart(t);
            scale = 1.0f + (0.08f * ease);
            alpha = 1.0f - ease;
        } else {
            // Phase 2: Zoom out into position (1.08 -> 1.0) + Fade In
            float ease = easeOutQuart(t);
            scale = 1.08f - (0.08f * ease);
            alpha = ease;
        }
        
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(width / 2f, height / 2f);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-width / 2f, -height / 2f);
    }

    public void endTransition(DrawContext context) {
        // We ALWAYS check if we pushed a matrix, and pop it even if transitioning has become false
        // (to prevent leaving a dirty matrix stack if the state changed mid-render).
        if (matrixPushedThisFrame) {
            context.getMatrices().popMatrix();
            matrixPushedThisFrame = false;
        }

        if (!transitioning) return;

        float t = MathHelper.clamp(progress, 0.0f, 1.0f);
        float alpha = isExiting ? (1.0f - easeInQuart(t)) : easeOutQuart(t);

        // Render black overlay on top of EVERYTHING to hide the hard screen switch
        int blackAlpha = (int)((1.0f - alpha) * 255);
        if (blackAlpha > 0) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            context.fill(-width, -height, width * 2, height * 2, (blackAlpha << 24) | 0x000000);
        }
    }

    private float easeOutQuart(float x) {
        return 1.0f - (float)Math.pow(1.0f - x, 4);
    }

    private float easeInQuart(float x) {
        return x * x * x * x;
    }

    public boolean isTransitioning() {
        return transitioning;
    }

    public void abortTransition() {
        transitioning = false;
        isExiting = false;
        isCompletingTransition = false;
        pendingScreen = null;
    }
}
