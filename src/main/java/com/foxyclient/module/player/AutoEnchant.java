package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** AutoEnchant - Auto enchants items at enchanting table. */
public class AutoEnchant extends Module {
    public AutoEnchant() { super("AutoEnchant", "Auto enchant at enchanting table", Category.PLAYER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!(mc.player.currentScreenHandler instanceof EnchantmentScreenHandler handler)) return;
        // Click highest available enchantment (slot 2 = level 3)
        for (int i = 2; i >= 0; i--) {
            if (handler.getLapisCount() >= i + 1 && mc.player.experienceLevel >= handler.enchantmentPower[i]) {
                mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
                return;
            }
        }
    }
}
