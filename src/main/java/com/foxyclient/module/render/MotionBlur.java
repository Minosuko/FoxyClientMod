package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Adds cinematic motion blur via a post-processing shader that blends
 * the current frame with previous frames.
 * 
 * Based on the noryea MotionBlur mod — reimplemented without the Satin API
 * dependency using vanilla Minecraft's PostEffectProcessor pipeline.
 */
import com.foxyclient.util.MotionBlurShader;

public class MotionBlur extends Module {
    public static MotionBlur INSTANCE;
    private final MotionBlurShader shader;

    public final NumberSetting strength = new NumberSetting(
        "Strength", "Blur amount (0 = off, 99 = max trails).", 50, 0, 99
    );

    public MotionBlur() {
        super("Motion Blur", "Adds smooth cinematic motion blur.", Category.RENDER);
        INSTANCE = this;
        this.shader = new MotionBlurShader();
        addSetting(strength);
    }

    @Override
    public void onEnable() {
        // Renderer will pick up enabled state next frame
    }

    @Override
    public void onDisable() {
        if (shader.isInitialized()) {
            shader.reload();
        }
    }

    public MotionBlurShader getShader() {
        return shader;
    }

    /**
     * Returns the blur blend factor (0.0 to 0.99).
     * Maps the user-facing 0-99 strength to a 0.0-0.99 blend factor
     * where higher = more persistence of previous frames.
     */
    public float getBlur() {
        return (float) Math.min(strength.get().intValue(), 99) / 100.0f;
    }
}
