package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WorldUtil;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Surrounds the player with obsidian for protection (Surround+).
 */
public class Surround extends Module {
    private final BoolSetting center = addSetting(new BoolSetting("Center", "Center player on block", true));
    private final BoolSetting floor = addSetting(new BoolSetting("Floor", "Place under feet", false));
    private final NumberSetting blocksPerTick = addSetting(new NumberSetting("BPT", "Blocks per tick", 4, 1, 8));
    private final BoolSetting predict = addSetting(new BoolSetting("Predict", "Predict movement", true));

    private boolean centered = false;

    public Surround() {
        super("Surround+", "Faster & smarter obsidian protection", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        centered = false;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (center.get() && !centered) {
            double x = Math.floor(mc.player.getX()) + 0.5;
            double z = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPosition(x, mc.player.getY(), z);
            centered = true;
        }

        int obsidianSlot = WorldUtil.findHotbarItem(Items.OBSIDIAN);
        if (obsidianSlot == -1) { error("No obsidian!"); return; }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] surround = {
            playerPos.north(), playerPos.south(),
            playerPos.east(), playerPos.west()
        };

        int placed = 0;
        for (BlockPos pos : surround) {
            if (placed >= blocksPerTick.get()) break;
            if (WorldUtil.placeBlock(pos, obsidianSlot, true)) {
                placed++;
            }
        }

        if (floor.get() && placed < blocksPerTick.get()) {
            WorldUtil.placeBlock(playerPos.down(), obsidianSlot, true);
        }
    }
}
