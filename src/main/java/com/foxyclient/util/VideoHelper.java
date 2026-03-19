package com.foxyclient.util;

import com.foxyclient.FoxyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles decoding `.mp4` video files to a dynamic NativeImageBackedTexture using JCodec (FrameGrab),
 * or loading static images like `.png` and `.jpg`.
 */
public class VideoHelper {
    public static final Identifier CUSTOM_BG_ID = Identifier.of("foxyclient", "custom_background");

    private static NativeImageBackedTexture bgTexture = null;
    private static Thread decodingThread = null;
    private static volatile boolean decodingRunning = false;
    private static boolean isStaticImage = false;
    private static boolean hasCustomBackground = false;

    // Double buffering for smooth playback
    private static NativeImage backBuffer = null;
    private static NativeImage frontBuffer = null;
    private static int[] pixelBuffer = null;
    private static final Object bufferLock = new Object();
    private static volatile boolean frameReady = false;

    public static boolean hasCustomBackground() {
        return hasCustomBackground;
    }

    public static Identifier getBackgroundId() {
        return CUSTOM_BG_ID;
    }

    /**
     * Initializes or reloads the custom background from FoxyConfig.
     */
    public static void initBackground() {
        stopVideo();
        MinecraftClient client = MinecraftClient.getInstance();
        
        String type = FoxyConfig.INSTANCE.customBackgroundType.get();
        
        if ("Default".equals(type)) {
            hasCustomBackground = false;
            return;
        }
        
        if ("FoxyClient".equals(type)) {
            isStaticImage = true;
            hasCustomBackground = true;
            loadBuiltinBackground(Identifier.of("foxyclient", "background.png"));
            return;
        }

        // Custom mode
        String bgPath = FoxyConfig.INSTANCE.customBackgroundPath.get();
        if (bgPath == null || bgPath.isEmpty()) {
            hasCustomBackground = false;
            return;
        }

        Path file = Paths.get(bgPath);
        if (!file.isAbsolute()) {
            file = client.runDirectory.toPath().resolve("config").resolve("foxyclient").resolve(bgPath);
        }
        
        if (!Files.exists(file)) {
            hasCustomBackground = false;
            return;
        }

        try {
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".mp4")) {
                isStaticImage = false;
                hasCustomBackground = true;
                startVideoDecoding(file);
            } else if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                isStaticImage = true;
                hasCustomBackground = true;
                loadStaticImage(file);
            } else {
                hasCustomBackground = false;
            }
        } catch (Exception e) {
            FoxyClient.LOGGER.error("Failed to load custom background", e);
            hasCustomBackground = false;
        }
    }

    /**
     * Called by MixinTitleScreen every frame prior to drawing.
     * Updates the OpenGL texture on the Render thread if a new video frame is ready.
     */
    public static void updateTexture() {
        if (!hasCustomBackground || isStaticImage) return;

        synchronized (bufferLock) {
            if (frameReady && frontBuffer != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (bgTexture == null || 
                    bgTexture.getImage().getWidth() != frontBuffer.getWidth() || 
                    bgTexture.getImage().getHeight() != frontBuffer.getHeight()) {
                    
                    if (bgTexture != null) bgTexture.close();
                    client.getTextureManager().destroyTexture(CUSTOM_BG_ID);
                    
                    // Create a copy for the texture and register.
                    // Subsequent updates will use upload().
                    NativeImage initialImage = new NativeImage(frontBuffer.getWidth(), frontBuffer.getHeight(), false);
                    initialImage.copyFrom(frontBuffer); 
                    bgTexture = new NativeImageBackedTexture(() -> "FoxyVideoTexture", initialImage);
                    client.getTextureManager().registerTexture(CUSTOM_BG_ID, bgTexture);
                } else {
                    bgTexture.getImage().copyFrom(frontBuffer);
                    bgTexture.upload();
                }
                frameReady = false;
            }
        }
    }

    private static void loadBuiltinBackground(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        try (InputStream is = client.getResourceManager().getResource(id).get().getInputStream()) {
            NativeImage image = NativeImage.read(is);
            if (bgTexture != null) bgTexture.close();
            client.getTextureManager().destroyTexture(CUSTOM_BG_ID);
            bgTexture = new NativeImageBackedTexture(() -> "FoxyFoxyBackground", image);
            client.getTextureManager().registerTexture(CUSTOM_BG_ID, bgTexture);
        } catch (Exception e) {
            FoxyClient.LOGGER.error("Failed to load builtin foxy background", e);
            hasCustomBackground = false;
        }
    }

    private static void loadStaticImage(Path file) throws Exception {
        MinecraftClient client = MinecraftClient.getInstance();
        try (InputStream is = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(is);
            if (bgTexture != null) bgTexture.close();
            client.getTextureManager().destroyTexture(CUSTOM_BG_ID);
            bgTexture = new NativeImageBackedTexture(() -> "FoxyStaticBackground", image);
            client.getTextureManager().registerTexture(CUSTOM_BG_ID, bgTexture);
        }
    }

    private static void startVideoDecoding(Path file) {
        decodingThread = new Thread(() -> {
            Thread thisThread = Thread.currentThread();
            try {
                while (decodingThread == thisThread) {
                    org.bytedeco.javacv.FFmpegFrameGrabber grab = null;
                    org.bytedeco.javacv.Java2DFrameConverter converter = null;
                    try {
                        grab = new org.bytedeco.javacv.FFmpegFrameGrabber(file.toFile());
                        // Enable hardware acceleration if available
                        grab.setVideoOption("hwaccel", "auto");
                        grab.start();

                        converter = new org.bytedeco.javacv.Java2DFrameConverter();
                        org.bytedeco.javacv.Frame frame;
                        
                        double fps = grab.getFrameRate();
                        if (fps < 1 || fps > 240) fps = 30.0;
                        
                        long startTime = System.nanoTime();
                        int frameCount = 0;

                        while (decodingThread == thisThread && (frame = grab.grabImage()) != null) {
                            while (frameReady && decodingThread == thisThread) {
                                Thread.yield(); // Spin-yield instead of Thread.sleep(1) to avoid 15ms Windows sleep penalty!
                            }
                            if (decodingThread != thisThread) break;

                            java.awt.image.BufferedImage bimg = converter.getBufferedImage(frame);
                            if (bimg != null) {
                                int w = bimg.getWidth();
                                int h = bimg.getHeight();

                                synchronized (bufferLock) {
                                    if (backBuffer == null || backBuffer.getWidth() != w || backBuffer.getHeight() != h) {
                                        if (backBuffer != null) backBuffer.close();
                                        if (frontBuffer != null) frontBuffer.close();
                                        backBuffer = new NativeImage(w, h, false);
                                        frontBuffer = new NativeImage(w, h, false);
                                        pixelBuffer = new int[w * h];
                                    }

                                    // Get RGB pixels robustly (always outputs TYPE_INT_ARGB format equivalents)
                                    bimg.getRGB(0, 0, w, h, pixelBuffer, 0, w);

                                    // Convert ARGB to ABGR in backBuffer
                                    int len = w * h;
                                    for (int i = 0; i < len; i++) {
                                        int argb = pixelBuffer[i];
                                        int a = (argb >> 24) & 0xFF;
                                        int r = (argb >> 16) & 0xFF;
                                        int g = (argb >> 8) & 0xFF;
                                        int b = argb & 0xFF;
                                        backBuffer.setColor(i % w, i / w, (a << 24) | (b << 16) | (g << 8) | r);
                                    }

                                    // Swap buffers
                                    NativeImage temp = frontBuffer;
                                    frontBuffer = backBuffer;
                                    backBuffer = temp;
                                    frameReady = true;
                                }

                                frameCount++;
                                long targetTimeNs = startTime + (long)(frameCount * (1_000_000_000.0 / fps));
                                long sleepTimeNs = targetTimeNs - System.nanoTime();
                                if (sleepTimeNs > 0) {
                                    long ms = sleepTimeNs / 1_000_000;
                                    if (ms > 5) {
                                        Thread.sleep(ms - 2); // Sleep slightly less than needed
                                    }
                                    while (System.nanoTime() < targetTimeNs) {
                                        Thread.yield(); // Spin-yield the remaining micro/nanoseconds for extreme precision
                                    }
                                } else {
                                    // If we're lagging behind target FPS, adjust internal clock so it doesn't spiral
                                    startTime -= sleepTimeNs; 
                                }
                            }
                        }
                    } catch (Exception loopException) {
                        if (decodingThread == thisThread) {
                            FoxyClient.LOGGER.error("Error during video loop decoding", loopException);
                            try { Thread.sleep(1000); } catch (InterruptedException e) {}
                        } else {
                            break;
                        }
                    } finally {
                        if (converter != null) {
                            converter.close();
                        }
                        if (grab != null) {
                            try {
                                grab.stop();
                                grab.release();
                            } catch (Exception e) {}
                        }
                    }
                }
            } catch (Exception e) {
                FoxyClient.LOGGER.error("Fatal error in video decoding thread", e);
            }
        }, "FoxyVideoDecoder");
        decodingThread.setDaemon(true);
        decodingThread.start();
    }

    public static void stopVideo() {
        if (decodingThread != null) {
            Thread t = decodingThread;
            decodingThread = null; // Instantly signals the thread to stop loop
            t.interrupt();
            try {
                t.join(500); // Wait up to 500ms for clean exit
            } catch (InterruptedException e) {}
        }
        synchronized (bufferLock) {
            if (backBuffer != null) {
                backBuffer.close();
                backBuffer = null;
            }
            if (frontBuffer != null) {
                frontBuffer.close();
                frontBuffer = null;
            }
            pixelBuffer = null;
        }
        frameReady = false;
    }
}
