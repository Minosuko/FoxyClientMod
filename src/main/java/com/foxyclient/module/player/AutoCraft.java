package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** AutoCraft - Auto crafts items when at a crafting table. */
public class AutoCraft extends Module {
    public AutoCraft() { super("AutoCraft", "Auto craft items at crafting table", Category.PLAYER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!(mc.player.currentScreenHandler instanceof CraftingScreenHandler handler)) return;
        var output = handler.getSlot(0).getStack();
        if (!output.isEmpty()) {
            mc.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}
