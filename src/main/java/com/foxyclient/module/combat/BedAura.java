package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Bed Aura - Places and detonates beds near targets in Nether/End.
 */
public class BedAura extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Place range", 4.5, 1.0, 6.0));

    public BedAura() {
        super("BedAura", "Bed explosions in Nether/End", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Only works where beds explode (nether/end)
        if (mc.world.getRegistryKey() != net.minecraft.world.World.OVERWORLD) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        int bedSlot = findBedSlot();
        if (bedSlot == -1) return;

        BlockPos placePos = findPlacement(target);
        if (placePos == null) return;

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = bedSlot;

        // Place bed
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(placePos), Direction.UP, placePos, false));

        // Right-click to detonate (beds explode in nether/end when used)
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(placePos.up()), Direction.UP, placePos.up(), false));

        mc.player.getInventory().selectedSlot = oldSlot;
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestDist = range.get() + 2;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dist = mc.player.distanceTo(p);
            if (dist < closestDist) { closestDist = dist; closest = p; }
        }
        return closest;
    }

    private BlockPos findPlacement(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = targetPos.add(x, 0, z);
                if (mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.up()).isAir()) {
                    if (!mc.world.getBlockState(pos.down()).isAir()) return pos.down();
                }
            }
        }
        return null;
    }

    private int findBedSlot() {
        for (int i = 0; i < 9; i++) {
            String name = mc.player.getInventory().getStack(i).getItem().toString();
            if (name.contains("bed")) return i;
        }
        return -1;
    }
}
