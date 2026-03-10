package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Automatically mines blocks in front of / around the player.
 */
public class AutoMine extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Mining range", 4.5, 1.0, 6.0));
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Mine mode", "Closest", "Closest", "Facing", "Surround"));

    private BlockPos currentTarget = null;

    public AutoMine() {
        super("AutoMine", "Auto mine blocks around you", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (currentTarget != null) {
            if (mc.world.getBlockState(currentTarget).isAir()) {
                currentTarget = null;
            } else {
                mc.interactionManager.updateBlockBreakingProgress(currentTarget, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
                return;
            }
        }

        // Find next target
        BlockPos playerPos = mc.player.getBlockPos();

        switch (mode.get()) {
            case "Facing" -> {
                if (mc.crosshairTarget instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                    BlockPos pos = bhr.getBlockPos();
                    if (pos.getSquaredDistance(playerPos) <= range.get() * range.get() && !mc.world.getBlockState(pos).isAir()) {
                        currentTarget = pos;
                    }
                }
            }
            case "Closest" -> {
                double closestDist = range.get() * range.get();
                int r = range.get().intValue();
                for (int x = -r; x <= r; x++) {
                    for (int y = -1; y <= 2; y++) {
                        for (int z = -r; z <= r; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
                            if (!mc.world.getBlockState(pos).isAir()) {
                                double dist = pos.getSquaredDistance(playerPos);
                                if (dist < closestDist) {
                                    closestDist = dist;
                                    currentTarget = pos;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
