package com.foxyclient.module.movement;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Removes the delay between jumps.
 */
public class NoJumpDelay extends Module {
    public NoJumpDelay() {
        super("NoJumpDelay", "Removes jump delay", Category.MOVEMENT);
    }
}
