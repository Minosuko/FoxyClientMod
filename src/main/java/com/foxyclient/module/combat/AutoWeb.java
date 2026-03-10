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

/**
 * Automatically places cobwebs on nearby enemies.
 */
public class AutoWeb extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 1.0, 6.0));

    public AutoWeb() {
        super("AutoWeb", "Places cobwebs on targets", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        int webSlot = WorldUtil.findHotbarItem(Items.COBWEB);
        if (webSlot == -1) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            BlockPos pos = player.getBlockPos();
            WorldUtil.placeBlock(pos, webSlot, true);
            WorldUtil.placeBlock(pos.up(), webSlot, true);
        }
    }
}
