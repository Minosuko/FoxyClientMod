package com.foxyclient.module.ui;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.Render2DEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.item.Items;

/**
 * Displays the number of totems in your inventory.
 */
public class TotemCounter extends Module {
    public TotemCounter() {
        super("TotemCounter", "Shows totem count", Category.UI);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;

        int totems = countTotems();
        String text = "§6Totems: §f" + totems;
        
        int x = 4;
        int y = mc.getWindow().getScaledHeight() - 48;
        
        event.getContext().drawTextWithShadow(mc.textRenderer, text, x, y, 0xFFFFFFFF);
    }

    private int countTotems() {
        int count = 0;
        for (int i = 0; i < 45; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                count += mc.player.getInventory().getStack(i).getCount();
            }
        }
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            count += mc.player.getOffHandStack().getCount();
        }
        return count;
    }
}
