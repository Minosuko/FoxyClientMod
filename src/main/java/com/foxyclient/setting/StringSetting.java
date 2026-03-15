package com.foxyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class StringSetting extends Setting<String> {
    private final boolean password;

    public StringSetting(String name, String description, String defaultValue) {
        this(name, description, defaultValue, false);
    }

    public StringSetting(String name, String description, String defaultValue, boolean password) {
        super(name, description, defaultValue);
        this.password = password;
    }

    public boolean isPassword() { return password; }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(get());
    }

    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonPrimitive()) set(element.getAsString());
    }
}
