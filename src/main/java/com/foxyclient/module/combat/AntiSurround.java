package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically breaks enemy surrounds.
 */
public class AntiSurround extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 1.0, 6.0));

    public AntiSurround() {
        super("AntiSurround", "Breaks enemy surrounds", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            BlockPos pos = player.getBlockPos();
            BlockPos[] surround = { pos.north(), pos.south(), pos.east(), pos.west() };

            for (BlockPos p : surround) {
                if (!mc.world.getBlockState(p).isAir()) {
                    mc.interactionManager.updateBlockBreakingProgress(p, net.minecraft.util.math.Direction.UP);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    return; // Break one at a time
                }
            }
        }
    }
}
