package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/** ShieldBypass - Attacks with an axe to disable shields, then swaps back. */
public class ShieldBypass extends Module {
    private int cooldown = 0;

    public ShieldBypass() { super("ShieldBypass", "Bypass shields with axe swap", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (cooldown > 0) { cooldown--; return; }
        if (mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;

        var target = mc.targetedEntity;
        if (target == null || !target.isAlive()) return;

        // Find axe
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.NETHERITE_AXE || stack.getItem() == Items.DIAMOND_AXE ||
                stack.getItem() == Items.IRON_AXE || stack.getItem() == Items.STONE_AXE) {
                int old = mc.player.getInventory().selectedSlot;
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.getInventory().selectedSlot = old;
                cooldown = 5;
                return;
            }
        }
    }
}
