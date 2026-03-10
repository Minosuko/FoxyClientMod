package com.foxyclient.module.world;
import com.foxyclient.module.Category; import com.foxyclient.module.Module;
import com.foxyclient.event.EventHandler; import com.foxyclient.event.events.TickEvent;
/** GhostBlockFixer - Fix ghost blocks by re-requesting block states. */
public class GhostBlockFixer extends Module {
    public GhostBlockFixer() { super("GhostBlockFixer", "Fix ghost blocks", Category.WORLD); }
}
