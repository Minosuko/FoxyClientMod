package com.foxyclient.module;

import com.foxyclient.FoxyClient;
import com.foxyclient.setting.Setting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Base module class. All FoxyClient features extend this.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private boolean visible = true;
    private int keybind;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public Module(String name, String description, Category category, int keybind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keybind = keybind;
    }

    public Module(String name, String description, Category category) {
        this(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
    }

    // Lifecycle
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) {
            FoxyClient.INSTANCE.getEventBus().register(this);
            onEnable();
        } else {
            FoxyClient.INSTANCE.getEventBus().unregister(this);
            onDisable();
        }
        
        // Don't register CustomKeybinds or ClickGUI as the last toggled to avoid loop/issues
        if (FoxyClient.INSTANCE != null && FoxyClient.INSTANCE.getModuleManager() != null && !this.getName().equals("CustomKeybinds") && !this.getName().equals("ClickGUI")) {
            FoxyClient.INSTANCE.getModuleManager().setLastToggledModule(this);
        }
    }

    // Settings
    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    // Chat helper
    protected void info(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[§bFoxyClient§7] §f" + message), false
            );
        }
    }

    protected void error(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[§bFoxyClient§7] §c" + message), false
            );
        }
    }

    protected boolean nullCheck() {
        return mc.player == null || mc.world == null;
    }

    // Getters/Setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }
    public List<Setting<?>> getSettings() { return settings; }
}
