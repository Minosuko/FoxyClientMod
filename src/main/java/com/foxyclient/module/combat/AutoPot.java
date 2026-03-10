package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.ThrowablePotionItem;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.util.Hand;

/** AutoPot - Automatically throws healing/buff splash potions. */
public class AutoPot extends Module {
    private final NumberSetting health = addSetting(new NumberSetting("Health", "Throw below HP", 10, 1, 20));
    private int delay = 0;

    public AutoPot() { super("AutoPot", "Auto throw splash potions", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (delay > 0) { delay--; return; }
        if (mc.player.getHealth() > health.get().floatValue()) return;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof ThrowablePotionItem) {
                int old = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                mc.player.setPitch(90);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = old;
                delay = 10;
                return;
            }
        }
    }
}
