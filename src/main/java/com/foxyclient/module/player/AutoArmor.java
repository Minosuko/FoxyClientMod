package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.ArmorUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Automatically equips the best armor from inventory.
 */
public class AutoArmor extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 2, 0, 20));

    private int timer = 0;

    public AutoArmor() {
        super("AutoArmor", "Auto equip best armor", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return; // Don't swap during screens

        if (timer > 0) {
            timer--;
            return;
        }

        // Slot mapping: 5=HEAD, 6=CHEST, 7=LEGS, 8=FEET
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = getSlot(i);
            if (slot == null) continue;

            ItemStack currentArmor = mc.player.getEquippedStack(slot);
            double currentScore = ArmorUtil.getProtectionScore(currentArmor);

            int bestSlot = -1;
            double bestScore = currentScore;

            // Scan inventory (0-35)
            // Inventory slots: 0-8 are hotbar, 9-35 are main
            for (int invSlot = 0; invSlot < 36; invSlot++) {
                ItemStack stack = mc.player.getInventory().getStack(invSlot);
                if (stack.isEmpty()) continue;

                if (ArmorUtil.getArmorSlot(stack) == slot) {
                    double score = ArmorUtil.getProtectionScore(stack);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSlot = invSlot;
                    }
                }
            }

            if (bestSlot != -1) {
                equipArmor(bestSlot, 5 + i); // Mapping i=0(HEAD)->5, i=1(CHEST)->6, i=2(LEGS)->7, i=3(FEET)->8
                timer = delay.get().intValue();
                return; // One action per tick/check
            }
        }
    }

    private EquipmentSlot getSlot(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.HEAD;
            case 1 -> EquipmentSlot.CHEST;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private void equipArmor(int invSlot, int armorSlot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        
        // Convert inventory slot to ScreenHandler slot
        // In PlayerScreenHandler:
        // 0: Crafting result
        // 1-4: Crafting grid
        // 5-8: Armor (HEAD, CHEST, LEGS, FEET)
        // 9-35: Main inventory
        // 36-44: Hotbar
        // 45: Offhand
        
        int handlerSlot = invSlot < 9 ? invSlot + 36 : invSlot;

        // Implementation of swap:
        // 1. Click item in inventory (PICKUP)
        // 2. Click armor slot (PICKUP) - this swaps the items if something was there
        // 3. Click inventory slot again (PICKUP) - this puts the old armor back if it was swapped
        
        mc.interactionManager.clickSlot(syncId, handlerSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
        
        // If the inventory slot wasn't empty after the swap, we need to put the old armor back
        // Actually, PICKUP on the armor slot will leave the old armor on the cursor.
        // So we need to click the inventory slot again to drop it.
        mc.interactionManager.clickSlot(syncId, handlerSlot, 0, SlotActionType.PICKUP, mc.player);
    }
}
