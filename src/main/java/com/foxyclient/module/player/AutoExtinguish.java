package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

/** AutoExtinguish - Automatically extinguishes when on fire. */
public class AutoExtinguish extends Module {
    public AutoExtinguish() { super("AutoExtinguish", "Auto extinguish when on fire", Category.PLAYER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.isOnFire()) {
            // Place water bucket below
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == net.minecraft.item.Items.WATER_BUCKET) {
                    slot = i; break;
                }
            }
            if (slot != -1) {
                int old = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = slot;
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = old;
            }
        }
    }
}
