package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.entity.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/** ChestAura - Automatically steals from nearby chests. */
public class ChestAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Search range", 4, 1, 6));
    private int delay = 0;

    public ChestAura() { super("ChestAura", "Auto steal from nearby chests", Category.COMBAT); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (delay > 0) { delay--; return; }

        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get().intValue();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    var be = mc.world.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) {
                        Vec3d hitVec = Vec3d.ofCenter(pos);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(hitVec, Direction.UP, pos, false));
                        delay = 20;
                        return;
                    }
                }
            }
        }
    }
}
