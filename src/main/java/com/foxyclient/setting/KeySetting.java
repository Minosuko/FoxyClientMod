package com.foxyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A setting specifically for keybinds.
 */
public class KeySetting extends Setting<Integer> {
    public KeySetting(String name, String description, Integer defaultValue) {
        super(name, description, defaultValue);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) {
            set(element.getAsInt());
        }
    }
}
