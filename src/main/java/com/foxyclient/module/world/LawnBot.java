package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** LawnBot - Auto breaks grass and flowers (JexClient port). */
public class LawnBot extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Clear range", 4, 1, 6));

    public LawnBot() { super("LawnBot", "Auto clear grass and plants", Category.WORLD); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        BlockPos center = mc.player.getBlockPos();
        int r = range.get().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    var block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS ||
                        block == Blocks.FERN || block == Blocks.LARGE_FERN ||
                        block == Blocks.DANDELION || block == Blocks.POPPY ||
                        block == Blocks.BLUE_ORCHID || block == Blocks.ALLIUM) {
                        mc.interactionManager.attackBlock(pos, Direction.UP);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        return;
                    }
                }
            }
        }
    }
}
