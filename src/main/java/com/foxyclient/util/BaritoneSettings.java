package com.foxyclient.util;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps core Baritone settings into the FoxyClient setting system.
 */
public class BaritoneSettings {
    public static final List<Setting<?>> SETTINGS = new ArrayList<>();

    public static final BoolSetting allowParkour = register(new BoolSetting("AllowParkour", "Allow Baritone to jump across gaps", BaritoneAPI.getSettings().allowParkour.value));
    public static final BoolSetting allowBreak = register(new BoolSetting("AllowBreak", "Allow Baritone to break blocks", BaritoneAPI.getSettings().allowBreak.value));
    public static final BoolSetting allowPlace = register(new BoolSetting("AllowPlace", "Allow Baritone to place blocks", BaritoneAPI.getSettings().allowPlace.value));
    public static final BoolSetting assumeWalkOnWater = register(new BoolSetting("AssumeWalkOnWater", "Assume Frost Walker or similar is active", BaritoneAPI.getSettings().assumeWalkOnWater.value));
    public static final BoolSetting renderGoal = register(new BoolSetting("RenderGoal", "Render the current pathfinding goal", BaritoneAPI.getSettings().renderGoal.value));

    private static <T extends Setting<?>> T register(T s) {
        SETTINGS.add(s);
        s.setOnChanged(v -> sync());
        return s;
    }

    public static void sync() {
        try {
            Settings s = BaritoneAPI.getSettings();
            s.allowParkour.value = allowParkour.get();
            s.allowBreak.value = allowBreak.get();
            s.allowPlace.value = allowPlace.get();
            s.assumeWalkOnWater.value = assumeWalkOnWater.get();
            s.renderGoal.value = renderGoal.get();
        } catch (Exception ignored) {}
    }
}
