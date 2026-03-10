package com.foxyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<String> {
    private final String[] modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = modes;
    }

    public String[] getModes() { return modes; }

    public void cycle() {
        String current = get();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(current)) {
                set(modes[(i + 1) % modes.length]);
                return;
            }
        }
    }

    public boolean is(String mode) {
        return get().equalsIgnoreCase(mode);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) set(element.getAsString());
    }
}
