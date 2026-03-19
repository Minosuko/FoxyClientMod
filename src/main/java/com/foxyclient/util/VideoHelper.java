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
 * Handles loading static images like `.png` and `.jpg` for custom backgrounds.
 * (Video support removed)
 */
public class VideoHelper {
    public static final Identifier CUSTOM_BG_ID = Identifier.of("foxyclient", "custom_background");

    private static NativeImageBackedTexture bgTexture = null;
    private static boolean isStaticImage = false;
    private static boolean hasCustomBackground = false;

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
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
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

    public static void updateTexture() {
        // No-op for static images. (Video support removed)
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

    public static void stopVideo() {
        // No-op (Video support removed)
    }
}
