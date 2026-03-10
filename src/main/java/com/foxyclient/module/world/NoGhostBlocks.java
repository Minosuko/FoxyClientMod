package com.foxyclient.module.world;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Prevents ghost blocks by sending extra block state requests.
 */
public class NoGhostBlocks extends Module {
    public NoGhostBlocks() {
        super("NoGhostBlocks", "Prevent ghost blocks", Category.WORLD);
    }

    // Anti-ghost-block works by resynchronizing block states with the server
    public boolean isAntiGhost() { return isEnabled(); }
}
