package com.foxyclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages external skin textures by loading them into Minecraft's TextureManager.
 */
public class SkinResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("FoxySkinManager");
    private static final Identifier CUSTOM_SKIN_ID = Identifier.of("foxyclient", "custom_player_skin");
    private static final Identifier CUSTOM_CAPE_ID = Identifier.of("foxyclient", "custom_player_cape");
    
    private static boolean skinLoaded = false;
    private static boolean capeLoaded = false;

    public static Identifier getCustomSkinId() {
        if (!skinLoaded) loadCustomSkin();
        return CUSTOM_SKIN_ID;
    }

    public static Identifier getCustomCapeId() {
        if (!capeLoaded) loadCustomCape();
        return CUSTOM_CAPE_ID;
    }

    public static void setCustomSkin(String sourcePath) {
        copyToCache(sourcePath, "custom_skin.png");
        skinLoaded = false;
        loadCustomSkin();
    }

    public static void setCustomCape(String sourcePath) {
        copyToCache(sourcePath, "custom_cape.png");
        capeLoaded = false;
        loadCustomCape();
    }

    public static void cacheDefaultCosmetics(Object texturesObj) {
        if (texturesObj == null) return;
        new Thread(() -> {
            try {
                // Discover methods on SkinTextures (Record or Class)
                java.lang.reflect.Method skinMethod = null;
                java.lang.reflect.Method capeMethod = null;
                
                for (java.lang.reflect.Method m : texturesObj.getClass().getMethods()) {
                    if (m.getParameterCount() == 0) {
                        String name = m.getName();
                        if (name.equals("body") || name.equals("skin")) skinMethod = m;
                        else if (name.equals("cape")) capeMethod = m;
                    }
                }

                if (skinMethod != null) {
                    Object skinAsset = skinMethod.invoke(texturesObj);
                    if (skinAsset != null) {
                        String url = findUrl(skinAsset);
                        if (url != null) {
                            downloadFile(url, "default_skin.png");
                            LOGGER.info("Cached default skin from {}", url);
                        }
                    }
                }

                if (capeMethod != null) {
                    Object capeAsset = capeMethod.invoke(texturesObj);
                    if (capeAsset != null) {
                        String url = findUrl(capeAsset);
                        if (url != null) {
                            downloadFile(url, "default_cape.png");
                            LOGGER.info("Cached default cape from {}", url);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to automatically cache default cosmetics", e);
            }
        }, "FoxyDefaultAutoCache").start();
    }

    private static String findUrl(Object asset) {
        try {
            // Try url() or textureUrl() or similar
            for (java.lang.reflect.Method m: asset.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("url") || m.getName().endsWith("Url"))) {
                    Object res = m.invoke(asset);
                    if (res instanceof String) return (String) res;
                    if (res instanceof java.util.Optional) {
                        java.util.Optional<?> opt = (java.util.Optional<?>) res;
                        if (opt.isPresent() && opt.get() instanceof String) return (String) opt.get();
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void rollbackToDefault(boolean skin, boolean cape) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
            
            if (skin) {
                Path defSkin = configDir.resolve("default_skin.png");
                if (Files.exists(defSkin)) {
                    Files.copy(defSkin, configDir.resolve("custom_skin.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    skinLoaded = false;
                    loadCustomSkin();
                }
            }
            if (cape) {
                Path defCape = configDir.resolve("default_cape.png");
                if (Files.exists(defCape)) {
                    Files.copy(defCape, configDir.resolve("custom_cape.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    capeLoaded = false;
                    loadCustomCape();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to rollback to default cosmetics", e);
        }
    }

    private static void downloadFile(String urlStr, String destName) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        try (java.io.InputStream is = url.openStream()) {
            MinecraftClient client = MinecraftClient.getInstance();
            Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
            Files.createDirectories(configDir);
            Path destFile = configDir.resolve(destName);
            Files.copy(is, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void copyToCache(String sourcePath, String destName) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
            Files.createDirectories(configDir);
            Path destFile = configDir.resolve(destName);
            Files.copy(Paths.get(sourcePath), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Cached {} to {}", sourcePath, destFile);
        } catch (Exception e) {
            LOGGER.error("Failed to copy texture {} to cache {}", sourcePath, destName, e);
        }
    }

    public static void loadCustomSkin() {
        loadTexture("custom_skin.png", CUSTOM_SKIN_ID, true);
    }

    public static void loadCustomCape() {
        loadTexture("custom_cape.png", CUSTOM_CAPE_ID, false);
    }

    private static void loadTexture(String filename, Identifier id, boolean isSkin) {
        MinecraftClient client = MinecraftClient.getInstance();
        Path configDir = client.runDirectory.toPath().resolve("config").resolve("foxyclient");
        Path file = configDir.resolve(filename);

        if (!Files.exists(file)) {
            LOGGER.warn("Custom texture file not found at: {}", file);
            return;
        }

        try (InputStream is = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(is);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> filename, image.getWidth(), image.getHeight(), false);
            texture.setImage(image);
            texture.upload();
            
            client.getTextureManager().registerTexture(id, texture);
            if (isSkin) skinLoaded = true;
            else capeLoaded = true;
            LOGGER.info("Successfully loaded custom texture from: {}", file);
        } catch (Exception e) {
            LOGGER.error("Failed to load custom texture {}", filename, e);
        }
    }

    public static void invalidate() {
        skinLoaded = false;
        capeLoaded = false;
    }
}
