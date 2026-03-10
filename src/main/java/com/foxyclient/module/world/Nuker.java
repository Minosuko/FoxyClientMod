package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Smarter and faster block breaking (BetterNuker).
 */
public class Nuker extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Break mode", "All", "All", "Flat", "Smash"));
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Break range", 5.0, 1.0, 6.0));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Blocks per tick", 2, 1, 20));

    public Nuker() {
        super("BetterNuker", "Faster block breaking", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int broken = 0;
        int r = range.get().intValue();

        for (int y = r; y >= -r; y--) { // Break top to bottom
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (broken >= speed.get()) return;

                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir() || state.getBlock() == Blocks.BEDROCK) continue;
                    if (mc.player.getEyePos().distanceTo(net.minecraft.util.math.Vec3d.ofCenter(pos)) > range.get()) continue;

                    switch (mode.get()) {
                        case "Flat" -> { if (pos.getY() < playerPos.getY()) continue; }
                        case "Smash" -> { if (state.getHardness(mc.world, pos) > 0.5f) continue; }
                    }

                    mc.interactionManager.attackBlock(pos, Direction.UP);
                    broken++;
                }
            }
        }
    }
}
