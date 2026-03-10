package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.WorldUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Advanced CrystalAura with improved placement and break timing (AutoCrystal+).
 */
public class CrystalAura extends Module {
    private final NumberSetting placeRange = addSetting(new NumberSetting("PlaceRange", "Crystal place range", 4.5, 1.0, 6.0));
    private final NumberSetting breakRange = addSetting(new NumberSetting("BreakRange", "Crystal break range", 4.5, 1.0, 6.0));
    private final NumberSetting minDamage = addSetting(new NumberSetting("MinDamage", "Minimum damage to target", 6.0, 0.0, 36.0));
    private final NumberSetting maxSelfDamage = addSetting(new NumberSetting("MaxSelfDamage", "Maximum damage to self", 8.0, 0.0, 36.0));
    private final NumberSetting speed = addSetting(new NumberSetting("Speed", "Action speed", 10.0, 1.0, 20.0));
    private final BoolSetting autoSwitch = addSetting(new BoolSetting("AutoSwitch", "Switch to end crystal", true));
    private final BoolSetting antiSuicide = addSetting(new BoolSetting("AntiSuicide", "Prevent self-kill", true));

    private int tickCounter = 0;

    public CrystalAura() {
        super("AutoCrystal+", "Advanced crystal PvP logic", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        tickCounter++;
        if (tickCounter < (20 / speed.get())) return;
        tickCounter = 0;

        // Break nearby crystals
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (mc.player.distanceTo(crystal) > breakRange.get()) continue;

            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        // Find targets and place
        PlayerEntity target = findTarget();
        if (target == null) return;

        int crystalSlot = WorldUtil.findHotbarItem(Items.END_CRYSTAL);
        if (crystalSlot == -1) return;

        BlockPos bestPos = findBestPlacement(target);
        if (bestPos == null) return;

        int oldSlot = mc.player.getInventory().selectedSlot;
        if (autoSwitch.get()) mc.player.getInventory().selectedSlot = crystalSlot;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
            new BlockHitResult(Vec3d.ofCenter(bestPos), Direction.UP, bestPos, false));

        if (autoSwitch.get()) mc.player.getInventory().selectedSlot = oldSlot;
    }

    private PlayerEntity findTarget() {
        PlayerEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;
            double dist = mc.player.distanceTo(player);
            if (dist < closestDist && dist < placeRange.get() + 2) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }

    private BlockPos findBestPlacement(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos bestPos = null;
        double bestDamage = 0;

        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (!canPlaceCrystal(pos)) continue;

                    double damage = estimateDamage(pos, target);
                    double selfDamage = estimateDamage(pos, mc.player);

                    if (antiSuicide.get() && selfDamage >= mc.player.getHealth()) continue;
                    if (selfDamage > maxSelfDamage.get()) continue;
                    if (damage < minDamage.get()) continue;

                    if (damage > bestDamage) {
                        bestDamage = damage;
                        bestPos = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        if (mc.world.getBlockState(pos).getBlock() != Blocks.OBSIDIAN &&
            mc.world.getBlockState(pos).getBlock() != Blocks.BEDROCK) return false;
        BlockPos above = pos.up();
        return mc.world.getBlockState(above).isAir() && mc.world.getBlockState(above.up()).isAir();
    }

    private double estimateDamage(BlockPos crystalPos, PlayerEntity target) {
        double dist = Math.sqrt(target.squaredDistanceTo(Vec3d.ofCenter(crystalPos)));
        if (dist > 12) return 0;
        return (1.0 - dist / 12.0) * 8.0;
    }
}
