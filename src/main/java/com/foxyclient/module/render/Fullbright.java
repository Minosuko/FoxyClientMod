package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.mixin.StatusEffectInstanceAccessor;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/**
 * FullBright module — makes everything fully visible regardless of light level.
 *
 * Modes:
 * - Gamma: Overrides the gamma value in the lightmap UBO to maximum brightness.
 * - Luminance: Overrides the gamma proportionally to a minimum light level setting.
 * - Potion: Applies a persistent Night Vision effect to the player.
 */
public class Fullbright extends Module {
    public enum Mode {
        Gamma,
        Luminance,
        Potion
    }

    private final ModeSetting mode = addSetting(
        new ModeSetting("Mode", "Brightness method", "Gamma", "Gamma", "Luminance", "Potion")
    );

    // Gamma settings
    private final NumberSetting brightness = addSetting(
        new NumberSetting("Brightness", "Gamma multiplier (higher = brighter)", 16.0, 1.0, 16.0)
    );

    // Luminance settings
    private final NumberSetting minimumLightLevel = addSetting(
        new NumberSetting("Min Light Level", "Minimum light level (0-15)", 8, 0, 15)
    );

    public Fullbright() {
        super("Fullbright", "Lights up your world!", Category.RENDER);

        mode.setOnChanged(val -> {
            if (isEnabled()) {
                if (!val.equals("Potion")) disableNightVision();
            }
        });
    }

    @Override
    public void onDisable() {
        if (mode.get().equals("Potion")) {
            disableNightVision();
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || !isEnabled()) return;

        if (mode.get().equals("Potion")) {
            if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                StatusEffectInstance instance = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                if (instance != null && instance.getDuration() < 420) {
                    ((StatusEffectInstanceAccessor) instance).setDuration(420);
                }
            } else {
                mc.player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.NIGHT_VISION, 420, 0, false, false, false)
                );
            }
        }
    }

    private void disableNightVision() {
        if (mc.player != null && mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    // --- Public API for the mixin ---

    public Mode getFullbrightMode() {
        try {
            return Mode.valueOf(mode.get());
        } catch (Exception e) {
            return Mode.Gamma;
        }
    }

    /**
     * Returns the gamma override value for the lightmap shader.
     * Called by MixinLightmapTextureManager when Gamma or Luminance mode is active.
     *
     * - Gamma mode: returns the user-configured brightness (1.0–16.0)
     * - Luminance mode: scales gamma proportionally to the minimum light level
     *   (level 15 = full gamma 16.0, level 0 = no override)
     */
    public double getGammaOverride() {
        if (mode.get().equals("Luminance")) {
            double minLevel = minimumLightLevel.get();
            // Scale: 0 → 0.0, 15 → 16.0
            return (minLevel / 15.0) * 16.0;
        }
        return brightness.get();
    }

    /**
     * Whether the mixin should apply a gamma override for this mode.
     */
    public boolean shouldOverrideGamma() {
        Mode m = getFullbrightMode();
        return m == Mode.Gamma || m == Mode.Luminance;
    }
}
