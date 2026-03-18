package com.foxyclient.util;

import com.foxyclient.FoxyClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages alternate accounts (offline mode).
 * Provides add/remove/login operations with JSON persistence.
 */
public class AltManager {
    private final List<AltAccount> alts = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    public AltManager() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("foxyclient").resolve("alts.json");
        load();
    }

    public List<AltAccount> getAlts() {
        return Collections.unmodifiableList(alts);
    }

    public void addAlt(String username) {
        // Don't add duplicates
        for (AltAccount alt : alts) {
            if (alt.getUsername().equalsIgnoreCase(username)) return;
        }
        alts.add(new AltAccount(username));
        save();
        FoxyClient.LOGGER.info("[FoxyClient] Alt added: {}", username);
    }

    public void removeAlt(String username) {
        alts.removeIf(a -> a.getUsername().equalsIgnoreCase(username));
        save();
        FoxyClient.LOGGER.info("[FoxyClient] Alt removed: {}", username);
    }

    /**
     * Switches the current session to the given alt account (offline mode).
     */
    public boolean login(AltAccount alt) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            UUID uuid = UUID.fromString(alt.getUuid());
            Session session = new Session(
                alt.getUsername(),
                uuid,
                "", // accessToken (empty for offline)
                Optional.empty(),
                Optional.empty()
            );
            // Set session via access widener
            mc.session = session;
            FoxyClient.LOGGER.info("[FoxyClient] Logged in as: {}", alt.getUsername());
            return true;
        } catch (Exception e) {
            FoxyClient.LOGGER.error("[FoxyClient] Failed to login as: {}", alt.getUsername(), e);
            return false;
        }
    }

    /**
     * Quick login by username.
     */
    public boolean login(String username) {
        for (AltAccount alt : alts) {
            if (alt.getUsername().equalsIgnoreCase(username)) {
                return login(alt);
            }
        }
        // Create temporary alt if not in list
        return login(new AltAccount(username));
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            JsonArray arr = new JsonArray();
            for (AltAccount alt : alts) {
                JsonObject obj = new JsonObject();
                obj.addProperty("username", alt.getUsername());
                obj.addProperty("uuid", alt.getUuid());
                arr.add(obj);
            }
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                gson.toJson(arr, writer);
            }
        } catch (IOException e) {
            FoxyClient.LOGGER.error("[FoxyClient] Failed to save alts", e);
        }
    }

    private void load() {
        if (!Files.exists(configPath)) return;
        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonArray arr = gson.fromJson(reader, JsonArray.class);
            if (arr == null) return;
            alts.clear();
            for (var el : arr) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                String name = obj.has("username") ? obj.get("username").getAsString() : null;
                String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : null;
                if (name != null) {
                    alts.add(uuid != null ? new AltAccount(name, uuid) : new AltAccount(name));
                }
            }
            FoxyClient.LOGGER.info("[FoxyClient] Loaded {} alts.", alts.size());
        } catch (Exception e) {
            FoxyClient.LOGGER.error("[FoxyClient] Failed to load alts", e);
        }
    }
}
