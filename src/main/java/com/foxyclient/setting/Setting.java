package com.foxyclient.setting;

import com.foxyclient.FoxyClient;
import com.google.gson.JsonElement;
import java.util.function.Consumer;

/**
 * Base setting class for module configuration.
 */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;
    private Consumer<T> onChanged;

    public Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    
    public void set(T value) { 
        this.value = value;
        if (onChanged != null) onChanged.accept(value);
        // Auto-save config
        if (FoxyClient.INSTANCE != null && FoxyClient.INSTANCE.getModuleManager() != null && !FoxyClient.INSTANCE.getModuleManager().isLoading()) {
            FoxyClient.INSTANCE.getModuleManager().saveConfig();
        }
    }
    
    public void setOnChanged(Consumer<T> onChanged) {
        this.onChanged = onChanged;
    }

    public T getDefaultValue() { return defaultValue; }
    public void reset() { this.value = defaultValue; }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
