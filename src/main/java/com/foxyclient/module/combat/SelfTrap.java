package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.WorldUtil;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Traps the player in obsidian for protection.
 */
public class SelfTrap extends Module {
    public SelfTrap() {
        super("SelfTrap", "Traps yourself for protection", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        int obsidianSlot = WorldUtil.findHotbarItem(Items.OBSIDIAN);
        if (obsidianSlot == -1) return;

        BlockPos pos = mc.player.getBlockPos();
        BlockPos[] trap = {
            pos.north().up(), pos.south().up(), pos.east().up(), pos.west().up(),
            pos.up(2)
        };

        for (BlockPos p : trap) {
            WorldUtil.placeBlock(p, obsidianSlot, true);
        }
    }
}
