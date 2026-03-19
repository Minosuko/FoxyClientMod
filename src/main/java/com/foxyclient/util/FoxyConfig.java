package com.foxyclient.util;

import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * FoxyClient Global Configuration.
 * Reimplemented to use the built-in Setting system and FabricLoader for persistence.
 */
public class FoxyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    public static final FoxyConfig INSTANCE = new FoxyConfig();

    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean loading = false;

    // Settings
    public final BoolSetting transitionsEnabled = register(new BoolSetting("transitionsEnabled", "Enable screen transitions", true));
    public final BoolSetting inGameTransitions = register(new BoolSetting("inGameTransitions", "Enable transitions when pressing ESC in-game", true));
    public final ModeSetting skinName = register(new ModeSetting("skinName", "Player skin", "Default", "Default", "Steve", "Alex", "Custom"));
    public final ModeSetting capeName = register(new ModeSetting("capeName", "Player cape", "Default", "None", "Custom"));
    public final BoolSetting slimModel = register(new BoolSetting("slimModel", "Slim (Alex) Model", false));
    public final com.foxyclient.setting.StringSetting customSkinPath = register(new com.foxyclient.setting.StringSetting("customSkinPath", "Custom Skin Path", ""));

    // Music Settings
    public final BoolSetting bgMusicEnabled = register(new BoolSetting("bgMusicEnabled", "Background Music", true));
    public final ModeSetting bgMusicType = register(new ModeSetting("bgMusicType", "Music Source", "Default", "Default", "Custom"));
    public final com.foxyclient.setting.StringSetting customMusicPath = register(new com.foxyclient.setting.StringSetting("customMusicPath", "Custom Music Path", ""));
    public final com.foxyclient.setting.StringSetting customMusicName = register(new com.foxyclient.setting.StringSetting("customMusicName", "Custom Music Name", ""));

    // Visual Settings
    public final ModeSetting customBackgroundType = register(new ModeSetting("customBackgroundType", "Background Type", "Default", "Default", "FoxyClient", "Custom"));
    public final com.foxyclient.setting.StringSetting customBackgroundPath = register(new com.foxyclient.setting.StringSetting("customBackgroundPath", "Custom Background Path", ""));
    public final com.foxyclient.setting.StringSetting customBackgroundName = register(new com.foxyclient.setting.StringSetting("customBackgroundName", "Custom Background Name", ""));
    
    public final ModeSetting customFontType = register(new ModeSetting("customFontType", "Font Type", "Default", "Default", "FoxyClient", "Custom"));
    public final com.foxyclient.setting.StringSetting customFontPath = register(new com.foxyclient.setting.StringSetting("customFontPath", "Custom Font Path", ""));
    public final com.foxyclient.setting.StringSetting customFontName = register(new com.foxyclient.setting.StringSetting("customFontName", "Custom Font Name", ""));
    
    // FoxyClient Account Info (Mock)
    // Account fields removed to prevent them from saving into shared FoxyConfig.json files

    private FoxyConfig() {
        // We use foxyconfig.json to avoid collision with ModuleManager's foxyclient.json
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve("foxyclient").resolve("foxyconfig.json");
    }

    private <T extends Setting<?>> T register(T setting) {
        settings.add(setting);
        // Auto-save on change, but only if not currently loading
        setting.setOnChanged(v -> {
            if (!loading) save();
        });
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save(); // Create default config if missing
            return;
        }

        loading = true;
        try {
            if (Files.size(configPath) == 0) return; // Empty file
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    for (Setting<?> setting : settings) {
                        JsonElement el = root.get(setting.getName());
                        if (el != null) {
                            try {
                                setting.fromJson(el);
                            } catch (Exception e) {
                                System.err.println("[FoxyClient] Error loading setting " + setting.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[FoxyClient] Failed to load FoxyConfig: " + e.getMessage());
            e.printStackTrace();
        } finally {
            loading = false;
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonObject root = new JsonObject();
            for (Setting<?> setting : settings) {
                root.add(setting.getName(), setting.toJson());
            }
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            System.err.println("[FoxyClient] Failed to save FoxyConfig: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
