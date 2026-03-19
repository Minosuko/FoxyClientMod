package com.foxyclient.module.ui;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.KeyEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows binding custom actions/commands to key combinations (macro system).
 */
public class CustomKeybinds extends Module {
    private final Map<Integer, String> macros = new HashMap<>();
    private final Path macrosFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public CustomKeybinds() {
        super("CustomKeybinds", "Custom key action bindings (macros)", Category.UI);
        macrosFile = FabricLoader.getInstance().getConfigDir().resolve("foxyclient").resolve("macros.json");
        loadMacros();
        setEnabled(true);
    }

    public void addMacro(int key, String command) {
        macros.put(key, command);
        saveMacros();
    }

    public void removeMacro(int key) {
        macros.remove(key);
        saveMacros();
    }

    public Map<Integer, String> getMacros() {
        return macros;
    }

    private void loadMacros() {
        if (Files.exists(macrosFile)) {
            try (Reader reader = Files.newBufferedReader(macrosFile)) {
                Map<Integer, String> loaded = gson.fromJson(reader, new TypeToken<Map<Integer, String>>(){}.getType());
                if (loaded != null) {
                    macros.clear();
                    macros.putAll(loaded);
                }
            } catch (Exception e) {
                error("Failed to load macros: " + e.getMessage());
            }
        }
    }

    private void saveMacros() {
        try {
            Files.createDirectories(macrosFile.getParent());
            try (Writer writer = Files.newBufferedWriter(macrosFile)) {
                gson.toJson(macros, writer);
            }
        } catch (IOException e) {
            error("Failed to save macros: " + e.getMessage());
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return; // Only trigger on press

        int key = event.getKey();
        if (macros.containsKey(key)) {
            String command = macros.get(key);
            if (command != null && !command.isEmpty()) {
                if (command.startsWith("/")) {
                    mc.getNetworkHandler().sendChatCommand(command.substring(1));
                } else if (command.startsWith(".")) {
                    // It's a client command, hand it to the CommandManager directly to avoid sending to server
                    FoxyClient.INSTANCE.getCommandManager().handleChat(command);
                } else if (command.startsWith("#")) {
                    FoxyClient.INSTANCE.getCommandManager().handleChat(command);
                } else {
                    mc.getNetworkHandler().sendChatMessage(command);
                }
            }
        }
    }
}
