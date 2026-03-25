package com.foxyclient.module.player;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.module.world.FoxyBot;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;

/**
 * AutoEat - Automatically eats food when hungry.
 * Supports both vanilla and modded food by reading FoodComponent data
 * directly from item stacks rather than checking item types.
 */
public class AutoEat extends Module {
    private final NumberSetting hungerThreshold = addSetting(new NumberSetting("Threshold", "Hunger level to eat at", 14, 1, 20));
    private final BoolSetting preferBest = addSetting(new BoolSetting("PreferBest", "Pick highest nutrition food", true));
    private final BoolSetting allowAlwaysEdible = addSetting(new BoolSetting("AlwaysEdible", "Allow always-edible food (gapples etc)", false));
    private final BoolSetting whileRunning = addSetting(new BoolSetting("WhileRunning", "Allow eating while FoxyBot is active", false));

    private boolean eating = false;
    private int previousSlot = -1;
    private boolean pausedFoxyBot = false;

    public AutoEat() {
        super("AutoEat", "Automatically eat when hungry", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Skip eating if an inventory screen is open (to avoid messing up clicks).
        // Standard menus (GameMenuScreen/ChatScreen) are allowed so eating continues.
        if (mc.currentScreen != null && !eating && mc.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) return;

        boolean foxyBotActive = isFoxyBotActive();

        // If FoxyBot is running and WhileRunning is off, skip
        if (foxyBotActive && !whileRunning.get() && !eating) return;

        int hunger = mc.player.getHungerManager().getFoodLevel();

        // Stop eating once full
        if (eating && hunger >= 20) {
            stopEating();
            return;
        }

        // Start eating if below threshold
        if (hunger <= hungerThreshold.get() && !eating) {
            int foodSlot = findBestFoodSlot();
            if (foodSlot == -1) return;

            // Pause FoxyBot pathing while we eat
            if (foxyBotActive && whileRunning.get()) {
                FoxyClient.INSTANCE.getPathFinder().cancelAll();
                pausedFoxyBot = true;
            }

            previousSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = foodSlot;
            mc.options.useKey.setPressed(true);
            mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
            eating = true;
        }

        // Continually enforce the keypress state to prevent window focus loss from clearing it
        if (eating) {
            mc.options.useKey.setPressed(true);
            if (!mc.player.isUsingItem()) {
                mc.interactionManager.interactItem(mc.player, net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }

    private boolean isFoxyBotActive() {
        FoxyBot foxyBot = FoxyClient.INSTANCE.getModuleManager().getModule(FoxyBot.class);
        boolean moduleActive = foxyBot != null && foxyBot.isEnabled();
        boolean pathingActive = FoxyClient.INSTANCE.getPathFinder() != null && FoxyClient.INSTANCE.getPathFinder().isAnyProcessActive();
        return moduleActive || pathingActive;
    }

    /**
     * Finds the best food slot in the hotbar by reading FoodComponent data.
     * Works with any item that has a FOOD component, including modded food.
     * If preferBest is on, picks the food with the highest effective value
     * (nutrition + saturation). Otherwise picks the first food found.
     */
    private int findBestFoodSlot() {
        int bestSlot = -1;
        float bestScore = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) continue;

            // Skip always-edible items (golden apples, chorus fruit) unless allowed
            if (food.canAlwaysEat() && !allowAlwaysEdible.get()) continue;

            if (!preferBest.get()) return i;

            // Score = nutrition + saturation (higher = better food)
            float score = food.nutrition() + food.saturation();
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private void stopEating() {
        mc.options.useKey.setPressed(false);
        if (previousSlot != -1) {
            mc.player.getInventory().selectedSlot = previousSlot;
            previousSlot = -1;
        }
        eating = false;

        // Resume FoxyBot if we paused it
        if (pausedFoxyBot) {
            pausedFoxyBot = false;
            FoxyBot foxyBot = FoxyClient.INSTANCE.getModuleManager().getModule(FoxyBot.class);
            if (foxyBot != null && foxyBot.isEnabled()) {
                // Re-toggle to re-dispatch the current mode
                foxyBot.setEnabled(false);
                foxyBot.setEnabled(true);
            }
        }
    }

    @Override
    public void onDisable() {
        if (eating) stopEating();
    }
}

