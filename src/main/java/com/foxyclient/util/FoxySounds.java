package com.foxyclient.util;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registers custom FoxyClient sound events.
 */
public class FoxySounds {
    public static final Identifier BACKGROUND_MUSIC_ID = Identifier.of("foxyclient", "background_music");
    public static final SoundEvent BACKGROUND_MUSIC = SoundEvent.of(BACKGROUND_MUSIC_ID);

    public static void register() {
        // No longer registering in global registries to avoid LAN sync issues.
        // The sound is still accessible via BACKGROUND_MUSIC_ID and BACKGROUND_MUSIC.
    }
}
