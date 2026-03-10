package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.item.*;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

/**
 * InventoryCleaner - Automatically trashes junk items and organizes inventory.
 * Drops useless items, stacks duplicates, and keeps only the best tools.
 */
public class InventoryCleaner extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks between actions", 5, 1, 20));
    private final BoolSetting dropTrash = addSetting(new BoolSetting("DropTrash", "Drop trash items", true));
    private final BoolSetting stackItems = addSetting(new BoolSetting("StackItems", "Auto-stack items", true));
    private final BoolSetting bestTools = addSetting(new BoolSetting("BestTools", "Keep only best tools", false));

    private int timer = 0;

    // Items considered trash
    private static final Set<Item> TRASH_ITEMS = Set.of(
        Items.ROTTEN_FLESH, Items.POISONOUS_POTATO, Items.SPIDER_EYE,
        Items.DEAD_BUSH, Items.DIRT, Items.COBBLESTONE, Items.GRAVEL,
        Items.SAND, Items.ANDESITE, Items.DIORITE, Items.GRANITE,
        Items.TUFF, Items.DEEPSLATE, Items.NETHERRACK, Items.COBBLED_DEEPSLATE
    );

    public InventoryCleaner() {
        super("InventoryCleaner", "Auto-clean inventory", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return;
        if (timer++ < delay.get().intValue()) return;
        timer = 0;

        // Drop trash items
        if (dropTrash.get()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && isTrash(stack)) {
                    mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        i, 1, SlotActionType.THROW, mc.player);
                    return; // One action per tick
                }
            }
        }

        // Stack duplicate items
        if (stackItems.get()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.isEmpty() || stack.getCount() >= stack.getMaxCount()) continue;

                for (int j = i + 1; j < 36; j++) {
                    ItemStack other = mc.player.getInventory().getStack(j);
                    if (ItemStack.areItemsEqual(stack, other)) {
                        // Click to pick up, then click to combine
                        mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            j, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            i, 0, SlotActionType.PICKUP, mc.player);
                        return;
                    }
                }
            }
        }

        // Keep only best tools (drop worse duplicates)
        if (bestTools.get()) {
            dropWorseTools(Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE);
            dropWorseTools(Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD);
            dropWorseTools(Items.DIAMOND_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE);
        }
    }

    private boolean isTrash(ItemStack stack) {
        return TRASH_ITEMS.contains(stack.getItem());
    }

    private void dropWorseTools(Item... tiers) {
        boolean foundGood = false;
        for (Item tier : tiers) {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == tier) {
                    if (foundGood) {
                        // Drop this worse tool
                        mc.interactionManager.clickSlot(
                            mc.player.currentScreenHandler.syncId,
                            i < 9 ? i + 36 : i, 1, SlotActionType.THROW, mc.player);
                        return;
                    }
                    foundGood = true;
                }
            }
        }
    }
}
