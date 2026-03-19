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

    // Custom Java Audio state
    private static Thread customAudioThread = null;
    private static volatile boolean customAudioRunning = false;
    private static SourceDataLine currentDataLine = null;

    public static void play() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSoundManager() == null) return;

        // Stop whatever is playing first if settings changed
        if (!FoxyConfig.INSTANCE.bgMusicEnabled.get()) {
            stop();
            return;
        }

        // Always stop vanilla default music tracker so it doesn't overlap
        if (mc.getMusicTracker() != null) {
            mc.getMusicTracker().stop();
        }

        String mode = FoxyConfig.INSTANCE.bgMusicType.get();
        if ("Default".equals(mode)) {
            stopCustomAudio(); // Ensure custom isn't playing
            if (vanillaMusicInstance == null || !mc.getSoundManager().isPlaying(vanillaMusicInstance)) {
                vanillaMusicInstance = new PositionedSoundInstance(
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
                mc.getSoundManager().play(vanillaMusicInstance);
            }
        } else if ("Custom".equals(mode)) {
            stopVanillaAudio(); // Ensure default isn't playing
            if (!customAudioRunning) {
                String path = FoxyConfig.INSTANCE.customMusicPath.get();
                if (!path.isEmpty()) {
                    File file = new File(path);
                    if (!file.isAbsolute()) {
                        file = new File(MinecraftClient.getInstance().runDirectory, "config/foxyclient/" + path);
                    }
                    playCustomAudio(file);
                }
            }
        }
    }

    public static void stop() {
        stopVanillaAudio();
        stopCustomAudio();
    }

    private static void stopVanillaAudio() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (vanillaMusicInstance != null && mc.getSoundManager() != null && mc.getSoundManager().isPlaying(vanillaMusicInstance)) {
            mc.getSoundManager().stop(vanillaMusicInstance);
        }
        vanillaMusicInstance = null;
    }

    private static void stopCustomAudio() {
        customAudioRunning = false;
        if (customAudioThread != null) {
            Thread t = customAudioThread;
            customAudioThread = null;
            t.interrupt();
            try {
                t.join(500);
            } catch (InterruptedException e) {}
        }
        if (currentDataLine != null) {
            currentDataLine.stop();
            currentDataLine.close();
            currentDataLine = null;
        }
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null && mc.currentScreen != null) {
            play(); // Ensure it's playing in menus
        } else if (mc.world != null) {
            stop(); // Ensure it stops in-game
        }

        // Apply volume adjustments to custom audio dynamically based on master & music sliders
        if (customAudioRunning && currentDataLine != null && currentDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            float masterVol = mc.options.getSoundVolume(SoundCategory.MASTER);
            float musicVol = mc.options.getSoundVolume(SoundCategory.MUSIC);
            float finalVol = masterVol * musicVol; // 0.0 to 1.0

            FloatControl gainControl = (FloatControl) currentDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            if (finalVol <= 0.001f) {
                gainControl.setValue(gainControl.getMinimum());
            } else {
                // Convert linear volume to decibels
                float db = (float) (Math.log10(finalVol) * 20.0f);
                gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), db)));
            }
        }
    }

    private static void playCustomAudio(File file) {
        if (!file.exists() || !file.isFile()) return;

        customAudioRunning = true;
        customAudioThread = new Thread(() -> {
            while (customAudioRunning) {
                try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
                    AudioFormat baseFormat = in.getFormat();
                    // Decode MP3/OGG to PCM
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
}
