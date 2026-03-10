package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** AutoFarm - Harvests and replants crops automatically. */
public class AutoFarm extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Farming range", 4, 1, 6));

    public AutoFarm() { super("AutoFarm", "Auto harvest and replant crops", Category.WORLD); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        BlockPos center = mc.player.getBlockPos();
        int r = range.get().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -1; y <= 1; y++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && crop.isMature(state)) {
                        mc.interactionManager.attackBlock(pos, Direction.UP);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        return;
                    }
                }
            }
        }
    }
}
