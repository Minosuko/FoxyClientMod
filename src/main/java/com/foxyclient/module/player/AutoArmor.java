package com.foxyclient.module.player;
import net.minecraft.entity.EquipmentSlot;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Automatically equips the best armor from inventory.
 */
public class AutoArmor extends Module {
    public AutoArmor() {
        super("AutoArmor", "Auto equip best armor", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return; // Don't swap during screens

        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            EquipmentSlot slot = switch(armorSlot) {
                case 0 -> EquipmentSlot.FEET;
                case 1 -> EquipmentSlot.LEGS;
                case 2 -> EquipmentSlot.CHEST;
                case 3 -> EquipmentSlot.HEAD;
                default -> null;
            };
            if (slot == null) continue;
            ItemStack currentArmor = mc.player.getEquippedStack(slot);
            int currentProtection = getProtection(currentArmor);

            int bestSlot = -1;
            int bestProtection = currentProtection;

            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.get(net.minecraft.component.DataComponentTypes.EQUIPPABLE) == null) continue;
                // Wait, EquipmentSlot for armor starts at 2 (FEET). 2-2=0, 3-2=1, etc.

                int protection = getProtection(stack);
                if (protection > bestProtection) {
                    bestProtection = protection;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1) {
                int syncId = mc.player.currentScreenHandler.syncId;
                int containerSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;

                // Swap armor
                if (!currentArmor.isEmpty()) {
                    mc.interactionManager.clickSlot(syncId, 8 - armorSlot, 0, SlotActionType.PICKUP, mc.player);
                }
                mc.interactionManager.clickSlot(syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(syncId, 8 - armorSlot, 0, SlotActionType.PICKUP, mc.player);
                if (!currentArmor.isEmpty()) {
                    mc.interactionManager.clickSlot(syncId, containerSlot, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
    }

    private int getProtection(ItemStack stack) {
        if (stack.get(net.minecraft.component.DataComponentTypes.EQUIPPABLE) == null) return 0;
        Integer dmg = stack.get(net.minecraft.component.DataComponentTypes.MAX_DAMAGE);
        return dmg != null ? dmg : 1;
    }
}
