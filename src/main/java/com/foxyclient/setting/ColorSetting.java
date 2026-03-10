package com.foxyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.awt.*;

/**
 * Color setting with RGBA support.
 */
public class ColorSetting extends Setting<Color> {
    public ColorSetting(String name, String description, Color defaultValue) {
        super(name, description, defaultValue);
    }

    public float[] getHSB() {
        Color c = get();
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    public int getAlpha() {
        return get().getAlpha();
    }

    public void setHSB(float h, float s, float b, int a) {
        Color c = Color.getHSBColor(h, s, b);
        set(new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
    }

    @Override
    public JsonElement toJson() {
        Color c = get();
        return new JsonPrimitive(String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
    }

    @Override
    public void fromJson(JsonElement json) {
        try {
            String hex = json.getAsString().replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            int a = hex.length() > 6 ? Integer.parseInt(hex.substring(6, 8), 16) : 255;
            set(new Color(r, g, b, a));
        } catch (Exception ignored) {}
    }
}
