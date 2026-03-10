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
 * Automatically breaks blocks to expose players in holes (City).
 */
public class AutoCity extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 5.0, 1.0, 10.0));

    public AutoCity() {
        super("AutoCity", "Breaks blocks to expose players", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            BlockPos pos = player.getBlockPos();
            BlockPos[] cityPoints = { pos.north(), pos.south(), pos.east(), pos.west() };

            for (BlockPos p : cityPoints) {
                if (mc.world.getBlockState(p).getBlock().getHardness() > 0) {
                    mc.interactionManager.updateBlockBreakingProgress(p, net.minecraft.util.math.Direction.UP);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        }
    }
}
