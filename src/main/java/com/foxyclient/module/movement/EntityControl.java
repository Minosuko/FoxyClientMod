package com.foxyclient.module.movement;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Allows controlling ridden entities regardless of ownership or status.
 */
public class EntityControl extends Module {
    public EntityControl() {
        super("EntityControl", "Control any ridden entity", Category.MOVEMENT);
    }
}
