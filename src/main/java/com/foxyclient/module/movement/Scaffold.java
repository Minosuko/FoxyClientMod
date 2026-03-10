package com.foxyclient.module.movement;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically places blocks beneath you (Scaffold+).
 */
public class Scaffold extends Module {
    private final BoolSetting tower = addSetting(new BoolSetting("Tower", "Place blocks upward", true));

    public Scaffold() {
        super("Scaffold+", "Advanced auto-bridging", Category.MOVEMENT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        BlockPos below = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(below).isAir()) return;

        int slot = findBlockSlot();
        if (slot == -1) return;

        if (WorldUtil.placeBlock(below, slot, true)) {
            if (tower.get() && mc.options.jumpKey.isPressed()) {
                mc.player.setVelocity(mc.player.getVelocity().x, 0.42, mc.player.getVelocity().z);
            }
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block != Blocks.TNT && block != Blocks.SAND && block != Blocks.GRAVEL) return i;
            }
        }
        return -1;
    }
}
