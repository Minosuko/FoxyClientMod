package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;

/** MossBot - Auto bonemeal moss blocks to farm moss/azalea (BleachHack port). */
public class MossBot extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Range", 4, 1, 6));
    private int delay = 0;

    public MossBot() { super("MossBot", "Auto bonemeal moss blocks", Category.WORLD); }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (delay++ < 5) return;
        delay = 0;

        int mealSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.BONE_MEAL) { mealSlot = i; break; }
        }
        if (mealSlot == -1) return;

        BlockPos center = mc.player.getBlockPos();
        int r = range.get().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -2; y <= 2; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.MOSS_BLOCK) {
                        int old = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = mealSlot;
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                            new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
                        mc.player.getInventory().selectedSlot = old;
                        return;
                    }
                }
            }
        }
    }
}
