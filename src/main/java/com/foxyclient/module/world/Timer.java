package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Changes game tick speed (game speed modifier).
 */
public class Timer extends Module {
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Tick speed multiplier", 2.0, 0.1, 10.0));

    public Timer() {
        super("Timer", "Change game speed", Category.WORLD);
    }

    public float getTickMultiplier() {
        return isEnabled() ? speed.get().floatValue() : 1.0f;
    }
}
