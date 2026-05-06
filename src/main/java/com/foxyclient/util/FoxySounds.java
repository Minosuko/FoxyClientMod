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
    
    public static final Identifier KURONAMI_ACE_ID = Identifier.of("foxyclient", "kuronami_ace");
    public static final SoundEvent KURONAMI_ACE = SoundEvent.of(KURONAMI_ACE_ID);

    public static final Identifier CHAMPIONS_2023_1_ID = Identifier.of("foxyclient", "champions_2023_finisher_1");
    public static final SoundEvent CHAMPIONS_2023_1 = SoundEvent.of(CHAMPIONS_2023_1_ID);

    public static final Identifier CHAMPIONS_2023_2_ID = Identifier.of("foxyclient", "champions_2023_finisher_2");
    public static final SoundEvent CHAMPIONS_2023_2 = SoundEvent.of(CHAMPIONS_2023_2_ID);

    public static final Identifier CHAMPIONS_2023_ACE_ID = Identifier.of("foxyclient", "champions_2023_ace");
    public static final SoundEvent CHAMPIONS_2023_ACE = SoundEvent.of(CHAMPIONS_2023_ACE_ID);

    public static final Identifier AEMONDIR_FINISHER_ID = Identifier.of("foxyclient", "aemondir_finisher");
    public static final SoundEvent AEMONDIR_FINISHER = SoundEvent.of(AEMONDIR_FINISHER_ID);

    public static final Identifier MYSTBLOOM_FINISHER_ID = Identifier.of("foxyclient", "mystbloom_finisher");
    public static final SoundEvent MYSTBLOOM_FINISHER = SoundEvent.of(MYSTBLOOM_FINISHER_ID);

    public static final Identifier NEOFRONTIER_FINISHER_ID = Identifier.of("foxyclient", "neofrontier_finisher");
    public static final SoundEvent NEOFRONTIER_FINISHER = SoundEvent.of(NEOFRONTIER_FINISHER_ID);

    public static void register() {
        // Do NOT register to Registries.SOUND_EVENT!
        // This causes registry sync failures when opening LAN to Vanilla players.
        // SoundManager can play via ID directly from sounds.json without global registry.
    }
}
