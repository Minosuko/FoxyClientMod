package com.foxyclient.module.player;

import com.foxyclient.event.EventHandler;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.event.events.TickEvent;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * Smarter tool and weapon switching (AutoTool+).
 */
public class AutoTool extends Module {
    private final BoolSetting weapons = addSetting(new BoolSetting("Weapons", "Switch to sword for entities", true));

    public AutoTool() {
        super("AutoTool+", "Smarter tool switching", Category.PLAYER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (!mc.player.handSwinging) return;
        if (mc.crosshairTarget == null) return;

        if (mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            handleMining(blockHit);
        } else if (weapons.get() && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult) {
            handleCombat();
        }
    }

    private void handleMining(net.minecraft.util.hit.BlockHitResult hit) {
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;

        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i = 0; i < 9; i++) {
            float speed = mc.player.getInventory().getStack(i).getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        if (bestSlot != -1) mc.player.getInventory().selectedSlot = bestSlot;
    }

    private void handleCombat() {
        int bestSlot = -1;
        double bestDamage = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.get(net.minecraft.component.DataComponentTypes.WEAPON) != null || stack.get(net.minecraft.component.DataComponentTypes.TOOL) != null) {
                // Simplified damage check
                bestSlot = i;
                break;
            }
        }
        if (bestSlot != -1) mc.player.getInventory().selectedSlot = bestSlot;
    }
}
