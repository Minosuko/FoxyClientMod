package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.item.Items;

/**
 * Automatically equips totems of undying from inventory.
 */
public class AutoTotem extends Module {
    public AutoTotem() {
        super("AutoTotem", "Automatically holds totems of undying", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        // Find totem in inventory
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                // Move to offhand
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId, i < 9 ? i + 36 : i, 0,
                    net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player
                );
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId, 45, 0,
                    net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player
                );
                mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId, i < 9 ? i + 36 : i, 0,
                    net.minecraft.screen.slot.SlotActionType.PICKUP, mc.player
                );
                break;
            }
        }
    }
}
