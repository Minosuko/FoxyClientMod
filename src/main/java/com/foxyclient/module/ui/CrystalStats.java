package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/**
 * Shows session statistics for End Crystals.
 */
public class CrystalStats extends Module {
    private int placed = 0;
    private int broken = 0;

    public CrystalStats() {
        super("CrystalStats", "Shows crystal stats", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        String text = "§6Crystals: §fP: " + placed + " §7/ §fB: " + broken;
        int x = 4;
        int y = mc.getWindow().getScaledHeight() - 60;
        
        event.getContext().drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFFFF);
    }
    
    // In a real implementation, we would hook into placement and breaking events
    public void onPlace() { placed++; }
    public void onBreak() { broken++; }
}
