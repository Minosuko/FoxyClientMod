package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Advanced situational offhand switching (Offhand+).
 */
public class Offhand extends Module {
    private final ModeSetting item = addSetting(new ModeSetting("Item", "Default offhand", "Totem",
        "Totem", "Crystal", "Gapple", "Shield"));
    private final NumberSetting healthSwitch = addSetting(new NumberSetting("Health", "Totem threshold", 10, 1, 20));

    public Offhand() {
        super("Offhand+", "Advanced situational offhand switching", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return;

        net.minecraft.item.Item needed = getItemNeeded();
        if (mc.player.getOffHandStack().getItem() == needed) return;

        int slot = findItemSlot(needed);
        if (slot != -1) {
            int windowSlot = slot < 9 ? slot + 36 : slot;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, windowSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, windowSlot, 0, SlotActionType.PICKUP, mc.player);
        }
    }

    private net.minecraft.item.Item getItemNeeded() {
        if (mc.player.getHealth() <= healthSwitch.get()) return Items.TOTEM_OF_UNDYING;
        return switch (item.get()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Gapple" -> Items.ENCHANTED_GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.TOTEM_OF_UNDYING;
        };
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
