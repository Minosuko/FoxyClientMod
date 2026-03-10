package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemSearch - Search for items across inventories and containers.
 * Scans the player's inventory and any open container screen for items
 * matching the search query. Reports matches via chat with slot positions.
 * Also searches inside shulker boxes if enabled.
 */
public class ItemSearch extends Module {
    private final BoolSetting highlight = addSetting(new BoolSetting("Highlight", "Highlight found items", true));
    private final BoolSetting shulkers = addSetting(new BoolSetting("Shulkers", "Search inside shulkers", true));

    private String searchQuery = "";
    private boolean searched = false;

    public ItemSearch() {
        super("ItemSearch", "Search for items in inventory", Category.MISC);
    }

    @Override
    public void onEnable() {
        searched = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (searched || searchQuery.isEmpty()) return;

        searched = true;
        List<SearchResult> results = new ArrayList<>();

        // Search player inventory (slots 0-35: 0-8 hotbar, 9-35 main)
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (matchesSearch(stack)) {
                String slotName = i < 9 ? "Hotbar " + (i + 1) : "Slot " + i;
                results.add(new SearchResult(slotName, stack));
            }

            // Search inside shulker boxes
            if (shulkers.get() && isShulkerBox(stack)) {
                ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
                if (container != null) {
                    int shulkerSlot = 0;
                    for (ItemStack inner : container.iterateNonEmpty()) {
                        if (matchesSearch(inner)) {
                            results.add(new SearchResult("Shulker(slot " + i + ") #" + shulkerSlot, inner));
                        }
                        shulkerSlot++;
                    }
                }
            }
        }

        // Search open container
        if (mc.currentScreen instanceof GenericContainerScreen screen) {
            int containerSlots = screen.getScreenHandler().slots.size() - 36;
            for (int i = 0; i < containerSlots; i++) {
                Slot slot = screen.getScreenHandler().getSlot(i);
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty() && matchesSearch(stack)) {
                    results.add(new SearchResult("Container #" + i, stack));
                }
            }
        }

        // Report results
        if (results.isEmpty()) {
            info("§cNo items found matching: §f" + searchQuery);
        } else {
            info("§aFound " + results.size() + " matches for: §f" + searchQuery);
            for (SearchResult r : results) {
                info("§7  " + r.location + ": §b" + r.stack.getName().getString()
                    + " §7x" + r.stack.getCount());
            }
        }
    }

    public boolean matchesSearch(ItemStack stack) {
        if (!isEnabled() || searchQuery.isEmpty()) return true;
        String name = stack.getName().getString().toLowerCase();
        String query = searchQuery.toLowerCase();

        // Support simple wildcards
        if (query.contains("*")) {
            String regex = query.replace("*", ".*");
            return name.matches(regex);
        }
        return name.contains(query);
    }

    private boolean isShulkerBox(ItemStack stack) {
        return stack.getItem() == Items.SHULKER_BOX ||
               stack.getItem() == Items.WHITE_SHULKER_BOX ||
               stack.getItem() == Items.ORANGE_SHULKER_BOX ||
               stack.getItem() == Items.MAGENTA_SHULKER_BOX ||
               stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX ||
               stack.getItem() == Items.YELLOW_SHULKER_BOX ||
               stack.getItem() == Items.LIME_SHULKER_BOX ||
               stack.getItem() == Items.PINK_SHULKER_BOX ||
               stack.getItem() == Items.GRAY_SHULKER_BOX ||
               stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX ||
               stack.getItem() == Items.CYAN_SHULKER_BOX ||
               stack.getItem() == Items.PURPLE_SHULKER_BOX ||
               stack.getItem() == Items.BLUE_SHULKER_BOX ||
               stack.getItem() == Items.BROWN_SHULKER_BOX ||
               stack.getItem() == Items.GREEN_SHULKER_BOX ||
               stack.getItem() == Items.RED_SHULKER_BOX ||
               stack.getItem() == Items.BLACK_SHULKER_BOX;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        this.searched = false;
    }
    public String getSearchQuery() { return searchQuery; }

    private record SearchResult(String location, ItemStack stack) {}
}
