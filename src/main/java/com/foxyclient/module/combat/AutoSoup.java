package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** AutoSoup - Eats mushroom stew for healing (Wurst port). */
public class AutoSoup extends Module {
    private final NumberSetting health = addSetting(new NumberSetting("Health", "Eat below HP", 12, 1, 20));

    public AutoSoup() { super("AutoSoup", "Auto eat soup/stew for healing", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.getHealth() > health.get().floatValue()) return;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.MUSHROOM_STEW ||
                mc.player.getInventory().getStack(i).getItem() == Items.SUSPICIOUS_STEW) {
                int old = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = old;
                return;
            }
        }
    }
}
