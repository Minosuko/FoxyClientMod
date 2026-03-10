package com.foxyclient.module.world;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Allows interacting with blocks through liquids (water/lava).
 */
public class LiquidInteract extends Module {
    public LiquidInteract() {
        super("LiquidInteract", "Interact through liquids", Category.WORLD);
    }

    public boolean shouldInteract() { return isEnabled(); }
}
