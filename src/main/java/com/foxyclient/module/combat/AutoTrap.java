package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WorldUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Automatically traps nearby enemies in obsidian or webs.
 */
public class AutoTrap extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Target range", 4.5, 1.0, 6.0));
    private final BoolSetting webs = addSetting(new BoolSetting("Webs", "Use webs if possible", false));

    public AutoTrap() {
        super("AutoTrap", "Automatically traps enemies", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        PlayerEntity target = getTarget();
        if (target == null) return;

        int obsidianSlot = WorldUtil.findHotbarItem(Items.OBSIDIAN);
        int webSlot = WorldUtil.findHotbarItem(Items.COBWEB);

        if (obsidianSlot == -1 && webSlot == -1) return;

        BlockPos targetPos = target.getBlockPos();
        BlockPos[] trapPositions = {
            targetPos.north(), targetPos.south(), targetPos.east(), targetPos.west(),
            targetPos.north().up(), targetPos.south().up(), targetPos.east().up(), targetPos.west().up(),
            targetPos.up(2)
        };

        int slot = webs.get() && webSlot != -1 ? webSlot : obsidianSlot;
        if (slot == -1) slot = obsidianSlot;
        if (slot == -1) return;

        for (BlockPos pos : trapPositions) {
            WorldUtil.placeBlock(pos, slot, true);
        }
    }

    private PlayerEntity getTarget() {
        PlayerEntity closest = null;
        double dist = range.get();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double d = mc.player.distanceTo(player);
            if (d < dist) {
                dist = d;
                closest = player;
            }
        }
        return closest;
    }
}
