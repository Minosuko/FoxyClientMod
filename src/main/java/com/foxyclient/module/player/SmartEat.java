package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * SmartEat - Intelligent auto-eating that picks the best food based on context.
 * Supports modded food by reading FoodComponent data from item stacks.
 * Prioritizes golden apples when low HP, otherwise picks highest-value food.
 * Avoids harmful items and respects combat/screen state.
 */
public class SmartEat extends Module {
    private final NumberSetting hungerThreshold = addSetting(new NumberSetting("Hunger", "Eat below this hunger", 14, 1, 20));
    private final NumberSetting healthThreshold = addSetting(new NumberSetting("GappleHP", "Use gapple below HP", 8, 1, 20));
    private final BoolSetting noCombat = addSetting(new BoolSetting("NoCombat", "Don't eat during combat", true));
    private final BoolSetting autoGapple = addSetting(new BoolSetting("AutoGapple", "Auto golden apple when low", true));
    private final BoolSetting avoidRaw = addSetting(new BoolSetting("AvoidRaw", "Avoid low-saturation food", true));
    private final NumberSetting minNutrition = addSetting(new NumberSetting("MinNutrition", "Minimum nutrition value to eat", 2, 0, 20));
    private final BoolSetting inventory = addSetting(new BoolSetting("Inventory", "Also search food from inventory", false));

    private boolean eating = false;
    private int prevSlot = -1;
    private boolean swappedFromInventory = false;

    public SmartEat() {
        super("SmartEat", "Intelligent auto-eating", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return;

        int hunger = mc.player.getHungerManager().getFoodLevel();
        float health = mc.player.getHealth();

        // Don't eat if full enough (unless we need emergency gapple)
        if (hunger >= hungerThreshold.get().intValue() && !(autoGapple.get() && health <= healthThreshold.get())) {
            if (eating) stopEating();
            return;
        }

        // Don't eat during combat if setting is on
        if (noCombat.get() && mc.player.hurtTime > 0) {
            if (eating) stopEating();
            return;
        }

        // Emergency golden apple when low HP
        if (autoGapple.get() && health <= healthThreshold.get()) {
            int gappleSlot = findGoldenApple();
            if (gappleSlot != -1) {
                if (gappleSlot >= 9) {
                    swapToHotbar(gappleSlot);
                    return; // Wait a tick for the swap to process
                }
                startEating(gappleSlot);
                return;
            }
        }

        // Normal eating - find best food dynamically
        if (hunger < hungerThreshold.get().intValue()) {
            int bestSlot = findBestFood(hunger);
            if (bestSlot != -1) {
                if (bestSlot >= 9) {
                    swapToHotbar(bestSlot);
                    return; // Wait a tick for the swap to process
                }
                startEating(bestSlot);
            }
        }
    }

    /**
     * Finds the best food in the hotbar (and optionally inventory) by scoring FoodComponent data.
     * Works with modded food since it reads nutrition/saturation dynamically.
     * Scoring: nutrition + saturation, with penalties for low-quality food.
     * Returns 0-8 for hotbar slots, 9-35 for inventory slots.
     */
    private int findBestFood(int currentHunger) {
        int bestSlot = -1;
        float bestScore = -1;
        int hungerMissing = 20 - currentHunger;
        int searchLimit = inventory.get() ? 36 : 9;

        for (int i = 0; i < searchLimit; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) continue;

            // Skip always-edible items during normal eating (they're special items)
            if (food.canAlwaysEat()) continue;

            int nutrition = food.nutrition();
            float saturation = food.saturation();

            // Skip food below minimum nutrition threshold
            if (nutrition < minNutrition.get().intValue()) continue;

            // Avoid low-saturation (raw) food if setting is on
            if (avoidRaw.get() && saturation < 1.0f && nutrition <= 3) continue;

            // Score: weighted combination of nutrition and saturation
            // Prefer food that fills without overfilling too much
            float score = nutrition + saturation;

            // Bonus for food that closely matches our hunger gap (less waste)
            if (nutrition <= hungerMissing) {
                score += 2.0f;
            }

            // Slight preference for hotbar items (avoid unnecessary swaps)
            if (i < 9) {
                score += 0.5f;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    /**
     * Finds golden apple or enchanted golden apple in hotbar (and optionally inventory).
     * Returns 0-8 for hotbar slots, 9-35 for inventory slots.
     */
    private int findGoldenApple() {
        int searchLimit = inventory.get() ? 36 : 9;
        for (int i = 0; i < searchLimit; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Swaps an inventory item to the current hotbar slot using pick-up + place interaction.
     * Uses the player inventory screen handler (syncId 0).
     * Inventory slot mapping: slots 9-35 map to screen handler slots 9-35,
     * hotbar slots 0-8 map to screen handler slots 36-44.
     */
    private void swapToHotbar(int inventorySlot) {
        if (mc.interactionManager == null) return;

        if (!eating) {
            prevSlot = mc.player.getInventory().selectedSlot;
            eating = true;
        }
        swappedFromInventory = true;

        int selectedHotbar = mc.player.getInventory().selectedSlot;
        // Screen handler slot IDs: inventory slots 9-35 stay as 9-35, hotbar 0-8 become 36-44
        int sourceScreenSlot = inventorySlot; // inventory slots 9-35 are the same in screen handler
        int targetScreenSlot = 36 + selectedHotbar;

        // Pick up from inventory slot, then place in hotbar slot
        mc.interactionManager.clickSlot(0, sourceScreenSlot, 0, SlotActionType.PICKUP, mc.player);
        mc.interactionManager.clickSlot(0, targetScreenSlot, 0, SlotActionType.PICKUP, mc.player);
        // If old hotbar item was there, put it back in inventory
        mc.interactionManager.clickSlot(0, sourceScreenSlot, 0, SlotActionType.PICKUP, mc.player);
    }

    private void startEating(int slot) {
        if (!eating) {
            prevSlot = mc.player.getInventory().selectedSlot;
            eating = true;
        }
        mc.player.getInventory().selectedSlot = slot;
        mc.options.useKey.setPressed(true);
    }

    private void stopEating() {
        mc.options.useKey.setPressed(false);
        if (swappedFromInventory && prevSlot != -1) {
            // The food we swapped in is now in selectedSlot; swap it back if there's still some left
            // We don't swap back since the food gets consumed; just restore the slot
            swappedFromInventory = false;
        }
        if (prevSlot != -1) {
            mc.player.getInventory().selectedSlot = prevSlot;
            prevSlot = -1;
        }
        eating = false;
    }

    @Override
    public void onDisable() {
        if (eating) stopEating();
    }
}

