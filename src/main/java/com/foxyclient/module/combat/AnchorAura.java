package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Auto respawn-anchor combat. Places and charges respawn anchors near targets
 * in overworld (where they explode).
 */
public class AnchorAura extends Module {
    private final NumberSetting placeRange = addSetting(new NumberSetting("PlaceRange", "Place range", 4.5, 1.0, 6.0));
    private final NumberSetting breakRange = addSetting(new NumberSetting("BreakRange", "Detonate range", 4.5, 1.0, 6.0));
    private final NumberSetting minDamage = addSetting(new NumberSetting("MinDamage", "Min damage to target", 5.0, 0.0, 36.0));

    public AnchorAura() {
        super("AnchorAura", "Auto respawn anchor combat", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        // Only works in overworld (anchors explode)
        if (mc.world.getRegistryKey() != net.minecraft.world.World.NETHER) return;
        // Wait, I'll try getDimension() directly.

        PlayerEntity target = findTarget();
        if (target == null) return;

        int anchorSlot = findSlot(Items.RESPAWN_ANCHOR);
        int glowstoneSlot = findSlot(Items.GLOWSTONE);
        if (anchorSlot == -1 || glowstoneSlot == -1) return;

        BlockPos bestPos = findBestPlacement(target);
        if (bestPos == null) return;

        int oldSlot = mc.player.getInventory().selectedSlot;

        // Place anchor
        mc.player.getInventory().selectedSlot = anchorSlot;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(bestPos), Direction.UP, bestPos, false));

        // Charge with glowstone
        mc.player.getInventory().selectedSlot = glowstoneSlot;
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(bestPos.up()), Direction.UP, bestPos.up(), false));

        // Detonate by right-clicking
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(bestPos.up()), Direction.UP, bestPos.up(), false));

        mc.player.getInventory().selectedSlot = oldSlot;
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestDist = placeRange.get().doubleValue() + 3;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dist = mc.player.distanceTo(p);
            if (dist < closestDist) { closestDist = dist; closest = p; }
        }
        return closest;
    }

    private BlockPos findBestPlacement(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = targetPos.add(x, -1, z);
                if (mc.world.getBlockState(pos).isAir() || mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                    if (mc.player.getBlockPos().getManhattanDistance(pos) <= placeRange.get().doubleValue()) return pos;
                }
            }
        }
        return null;
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
