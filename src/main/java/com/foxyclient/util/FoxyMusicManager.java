package com.foxyclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.random.Random;

import javax.sound.sampled.*;
import java.io.File;

/**
 * Manages the custom FoxyClient background music globally.
 * Supports Vanilla OpenAL (for Default) and javax.sound.sampled (for Custom WAV/MP3/OGG).
 */
public class FoxyMusicManager {
    private static SoundInstance vanillaMusicInstance = null;
    private static boolean vanillaStartedThisTick = false;

    // Custom Java Audio state
    private static Thread customAudioThread = null;
    private static volatile boolean customAudioRunning = false;
    private static SourceDataLine currentDataLine = null;

    // Debounce: prevent rapid stop/play cycles from killing audio
    private static long lastStopTime = 0;
    private static final long STOP_DEBOUNCE_MS = 200;

    public static void play() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;

        boolean inGame = mc.world != null;
        boolean menuMusic = FoxyConfig.INSTANCE.menuMusicEnabled.get();
        com.foxyclient.module.render.MusicPlayer player = getPlayerModule();
        boolean playerActive = player != null && player.isEnabled();

        if (inGame) {
            if (!playerActive) {
                stop();
                return;
            }
        } else {
            if (!menuMusic) {
                stop();
                return;
            }
        }

        // Suppress vanilla music tracker so it doesn't overlap
        try {
            if (mc.getMusicTracker() != null) {
                mc.getMusicTracker().stop();
            }
        } catch (Exception ignored) {}

        String mode = FoxyConfig.INSTANCE.bgMusicType.get();
        if ("Default".equals(mode)) {
            stopCustomAudio();
            playVanillaAudio(mc, player);
        } else if ("Custom".equals(mode)) {
            stopVanillaAudio();
            playCustomMode(mc);
        }
    }

    private static void playVanillaAudio(MinecraftClient mc, com.foxyclient.module.render.MusicPlayer player) {
        // Check if our instance is still playing
        if (vanillaMusicInstance != null) {
            try {
                if (mc.getSoundManager().isPlaying(vanillaMusicInstance)) {
                    return; // Already playing, don't restart
                }
            } catch (Exception ignored) {}
        }

        // Don't spam-create instances (debounce)
        if (vanillaStartedThisTick) return;
        vanillaStartedThisTick = true;

        float moduleVol = (player != null) ? player.volume.get().floatValue() : 1.0f;
        moduleVol = Math.max(0.0f, Math.min(moduleVol, 2.0f));

        try {
            vanillaMusicInstance = new PositionedSoundInstance(
                FoxySounds.BACKGROUND_MUSIC.id(),
                SoundCategory.MUSIC,
                moduleVol,
                1.0f,
                Random.create(),
                true, // repeat
                0,
                SoundInstance.AttenuationType.NONE,
                0.0D, 0.0D, 0.0D,
                true // relative
            );
            mc.getSoundManager().play(vanillaMusicInstance);
        } catch (Exception e) {
            System.err.println("[FoxyMusicManager] Failed to play vanilla audio: " + e.getMessage());
            e.printStackTrace();
            vanillaMusicInstance = null;
        }
    }

    private static void playCustomMode(MinecraftClient mc) {
        if (customAudioRunning) return; // Already playing

        String path = FoxyConfig.INSTANCE.customMusicPath.get();
        if (path == null || path.isEmpty()) return;

        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(mc.runDirectory, "config/foxyclient/" + path);
        }
        playCustomAudio(file);
    }

    public static void stop() {
        lastStopTime = System.currentTimeMillis();
        stopVanillaAudio();
        stopCustomAudio();
    }

    /**
     * Debounced stop — used by screen transitions to prevent killing music
     * during transient screen == null states.
     */
    public static void softStop() {
        // Only stop if the MusicPlayer module is actually OFF
        com.foxyclient.module.render.MusicPlayer player = getPlayerModule();
        boolean playerActive = player != null && player.isEnabled();
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc != null && mc.world != null && playerActive) {
            // Player is in-game with MusicPlayer enabled — don't stop
            return;
        }

        boolean menuMusic = FoxyConfig.INSTANCE.menuMusicEnabled.get();
        if (mc != null && mc.world == null && menuMusic) {
            // In menu with menu music on — don't stop
            return;
        }

        stop();
    }

    private static void stopVanillaAudio() {
        if (vanillaMusicInstance != null) {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getSoundManager() != null) {
                    mc.getSoundManager().stop(vanillaMusicInstance);
                }
            } catch (Exception ignored) {}
            vanillaMusicInstance = null;
        }
    }

    private static void stopCustomAudio() {
        customAudioRunning = false;
        if (customAudioThread != null) {
            Thread t = customAudioThread;
            customAudioThread = null;
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException ignored) {}
        }
        if (currentDataLine != null) {
            try {
                currentDataLine.stop();
                currentDataLine.close();
            } catch (Exception ignored) {}
            currentDataLine = null;
        }
    }

    public static void tick() {
        vanillaStartedThisTick = false; // Reset per-tick debounce

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        boolean inGame = mc.world != null;
        boolean menuMusic = FoxyConfig.INSTANCE.menuMusicEnabled.get();
        com.foxyclient.module.render.MusicPlayer player = getPlayerModule();
        boolean playerActive = player != null && player.isEnabled();

        // Determine if music should be playing
        boolean shouldPlay = false;
        if (inGame && playerActive) {
            shouldPlay = true;
        } else if (!inGame && mc.currentScreen != null && menuMusic) {
            shouldPlay = true;
        }

        if (shouldPlay) {
            play();
        } else {
            // Only stop if we're not in a debounce window (prevents transient stops)
            long elapsed = System.currentTimeMillis() - lastStopTime;
            if (elapsed > STOP_DEBOUNCE_MS) {
                stop();
            }
        }

        // Apply volume adjustments to custom audio dynamically
        if (customAudioRunning && currentDataLine != null) {
            try {
                float masterVol = mc.options.getSoundVolume(SoundCategory.MASTER);
                float musicVol = mc.options.getSoundVolume(SoundCategory.MUSIC);
                float moduleVol = (player != null) ? player.volume.get().floatValue() : 1.0f;
                float finalVol = masterVol * musicVol * moduleVol;

                if (currentDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) currentDataLine.getControl(FloatControl.Type.MASTER_GAIN);
                    if (finalVol <= 0.001f) {
                        gainControl.setValue(gainControl.getMinimum());
                    } else {
                        float db = (float) (Math.log10(Math.min(finalVol, 2.0f)) * 20.0f);
                        gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), db)));
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static void playCustomAudio(File file) {
        if (!file.exists() || !file.isFile()) {
            System.err.println("[FoxyMusicManager] Custom audio file not found: " + file.getAbsolutePath());
            return;
        }

        customAudioRunning = true;
        customAudioThread = new Thread(() -> {
            while (customAudioRunning) {
                try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
                    AudioFormat baseFormat = in.getFormat();
                    // Decode compressed formats (MP3/OGG) to PCM
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            false
                    );

                    try (AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in)) {
                        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                        currentDataLine = line;
                        line.open(decodedFormat);
                        line.start();

                        byte[] data = new byte[4096];
                        int bytesRead;
                        while (customAudioRunning && (bytesRead = din.read(data, 0, data.length)) != -1) {
                            line.write(data, 0, bytesRead);
                        }

                        line.drain();
                        line.stop();
                        line.close();
                        if (currentDataLine == line) currentDataLine = null;
                    }
                } catch (UnsupportedAudioFileException e) {
                    System.err.println("[FoxyMusicManager] Unsupported audio format: " + file.getName() + " - " + e.getMessage());
                    customAudioRunning = false;
                    break;
                } catch (Exception e) {
                    System.err.println("[FoxyMusicManager] Error playing custom audio: " + e.getMessage());
                    e.printStackTrace();
                    customAudioRunning = false;
                    break;
                }
            }
        }, "FoxyCustomAudioStream");
        customAudioThread.setDaemon(true);
        customAudioThread.start();
    }

    public static String getCurrentTrackName() {
        String mode = FoxyConfig.INSTANCE.bgMusicType.get();
        if ("Default".equals(mode)) return "Foxy Theme";
        if ("Custom".equals(mode)) {
            String name = FoxyConfig.INSTANCE.customMusicName.get();
            return (name != null && !name.isEmpty()) ? name : "Custom Track";
        }
        return "None";
    }

    public static boolean isPlaying() {
        if (customAudioRunning) return true;
        if (vanillaMusicInstance != null) {
            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getSoundManager() != null) {
                    return mc.getSoundManager().isPlaying(vanillaMusicInstance);
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static com.foxyclient.module.render.MusicPlayer getPlayerModule() {
        try {
            if (com.foxyclient.FoxyClient.INSTANCE != null
                && com.foxyclient.FoxyClient.INSTANCE.getModuleManager() != null) {
                return com.foxyclient.FoxyClient.INSTANCE.getModuleManager()
                    .getModule(com.foxyclient.module.render.MusicPlayer.class);
            }
        } catch (Exception ignored) {}
        return null;
    }
}

