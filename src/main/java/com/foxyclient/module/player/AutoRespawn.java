package com.foxyclient.module.player;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Automatically respawn when killed (clicks respawn button).
 */
public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Auto respawn on death", Category.PLAYER);
    }

    // Applied via MixinDeathScreen or screen check
    public boolean shouldAutoRespawn() {
        return isEnabled();
    }
}
