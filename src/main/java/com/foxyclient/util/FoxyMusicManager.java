package com.foxyclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

/**
 * Manages the custom FoxyClient background music globally.
 */
public class FoxyMusicManager {
    private static SoundInstance foxyMusicInstance = null;

    public static void play() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;

        // Stop vanilla music
        if (mc.getMusicTracker() != null) {
            mc.getMusicTracker().stop();
        }

        // Play custom music if not already playing
        if (foxyMusicInstance == null || !mc.getSoundManager().isPlaying(foxyMusicInstance)) {
            foxyMusicInstance = new PositionedSoundInstance(
                FoxySounds.BACKGROUND_MUSIC.id(),
                SoundCategory.MUSIC,
                1.0f,
                1.0f,
                Random.create(),
                true, // repeat
                0,
                SoundInstance.AttenuationType.NONE,
                0.0D, 0.0D, 0.0D,
                true // relative
            );
            mc.getSoundManager().play(foxyMusicInstance);
        }
    }

    public static void stop() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (foxyMusicInstance != null && mc.getSoundManager() != null && mc.getSoundManager().isPlaying(foxyMusicInstance)) {
            mc.getSoundManager().stop(foxyMusicInstance);
        }
        foxyMusicInstance = null;
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null && mc.currentScreen != null) {
            play(); // Ensure it's playing in menus
        } else if (mc.world != null) {
            stop(); // Ensure it stops in-game
        }
    }
}
