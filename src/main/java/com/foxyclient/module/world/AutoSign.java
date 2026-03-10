package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;

/**
 * Auto-writes text on signs.
 */
public class AutoSign extends Module {
    private String[] lines = {"FoxyClient", "was here", "", ""};

    public AutoSign() {
        super("AutoSign", "Auto write on signs", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        // AutoSign applies text when sign GUI opens
    }

    public String[] getLines() { return lines; }
    public void setLines(String[] lines) { this.lines = lines; }
}
