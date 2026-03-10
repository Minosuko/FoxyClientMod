package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WorldUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fills holes near enemies to prevent them from camping.
 */
public class HoleFill extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 5.0, 1.0, 10.0));

    public HoleFill() {
        super("HoleFill+", "Fills holes near enemies", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        int obsidianSlot = WorldUtil.findHotbarItem(Items.OBSIDIAN);
        if (obsidianSlot == -1) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            BlockPos pos = player.getBlockPos();
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        BlockPos checkPos = pos.add(x, y, z);
                        if (WorldUtil.isHole(checkPos)) {
                            WorldUtil.placeBlock(checkPos, obsidianSlot, true);
                        }
                    }
                }
            }
        }
    }
}
