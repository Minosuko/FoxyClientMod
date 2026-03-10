package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/** AutoGrind - Auto uses grindstone to remove enchantments for XP. */
public class AutoGrind extends Module {
    public AutoGrind() { super("AutoGrind", "Auto grindstone for XP", Category.PLAYER); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!(mc.player.currentScreenHandler instanceof GrindstoneScreenHandler handler)) return;
        if (!handler.getSlot(2).getStack().isEmpty()) {
            mc.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}
