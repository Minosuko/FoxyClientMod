package com.foxyclient.module.player;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;

/**
 * Extends interaction reach distance.
 */
public class Reach extends Module {
    private final NumberSetting reach = addSetting(new NumberSetting("Reach", "Reach distance", 5.0, 3.0, 8.0));

    public Reach() {
        super("Reach", "Extend interaction reach", Category.PLAYER);
    }

    public double getReach() {
        return reach.get();
    }
}
