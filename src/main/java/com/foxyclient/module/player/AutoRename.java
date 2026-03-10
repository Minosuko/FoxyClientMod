package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.component.DataComponentTypes;

/** 
 * AutoRename - Auto renames items at anvil. 
 */
public class AutoRename extends Module {

    private String targetName = "Foxy Client";
    
    // Store ticks since we opened the anvil/renamed to avoid spamming packets
    private int delay = 0;

    public AutoRename() { 
        super("AutoRename", "Auto rename items at anvil", Category.PLAYER); 
    }

    @Override
    public void onEnable() {
        delay = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (delay > 0) {
            delay--;
            return;
        }

        // Check if we are currently inside an Anvil GUI
        if (mc.player.currentScreenHandler instanceof AnvilScreenHandler anvilHandler) {
            
            // Slot 0: Input, Slot 1: Material, Slot 2: Output
            ItemStack inputItem = anvilHandler.getSlot(0).getStack();
            ItemStack outputItem = anvilHandler.getSlot(2).getStack();

            String name = targetName;

            if (!inputItem.isEmpty()) {
                // Determine the current custom name. 
                // In 1.21 this is handled via DataComponentTypes.CUSTOM_NAME.
                String currentName = "";
                if (inputItem.contains(DataComponentTypes.CUSTOM_NAME)) {
                    currentName = inputItem.get(DataComponentTypes.CUSTOM_NAME).getString();
                } else {
                    currentName = inputItem.getName().getString();
                }

                // If the name is already correct, do nothing
                if (currentName.equals(name)) {
                    return;
                }

                // 1. Tell server to set the new name
                mc.getNetworkHandler().sendPacket(new RenameItemC2SPacket(name));
                
                // 2. Also manually update the handler client-side so output slot calculates
                anvilHandler.setNewItemName(name);

                // 3. If the output slot has the item, click it to retrieve it (requires XP!)
                if (!outputItem.isEmpty()) {
                    // Shift click the output slot (Slot 2)
                    mc.interactionManager.clickSlot(
                        anvilHandler.syncId, 
                        2, 
                        0, 
                        SlotActionType.QUICK_MOVE, 
                        mc.player
                    );
                    
                    // Add a tiny delay so we don't spam renaming on the next tick
                    delay = 5;
                }
            }
        }
    }
}
