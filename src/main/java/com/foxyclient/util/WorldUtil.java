package com.foxyclient.util;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * World interaction utilities.
 */
public class WorldUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean placeBlock(BlockPos pos, int slot, boolean rotate) {
        if (mc.player == null || mc.world == null) return false;
        if (!mc.world.getBlockState(pos).isReplaceable()) return false;

        Direction side = getPlaceSide(pos);
        if (side == null) return false;

        BlockPos neighbor = pos.offset(side);
        Direction opposite = side.getOpposite();
        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(Vec3d.of(opposite.getVector()).multiply(0.5));

        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;

        // TODO: Handle rotation if needed (packet-based or client-side)
        
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, opposite, neighbor, false));
        mc.player.swingHand(Hand.MAIN_HAND);

        mc.player.getInventory().selectedSlot = oldSlot;
        return true;
    }

    private static Direction getPlaceSide(BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.offset(side);
            if (!mc.world.getBlockState(neighbor).isAir() && !mc.world.getBlockState(neighbor).isReplaceable()) {
                return side;
            }
        }
        return null;
    }

    public static int findHotbarItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }

    public static boolean isHole(BlockPos pos) {
        if (!mc.world.getBlockState(pos).isAir()) return false;
        if (!mc.world.getBlockState(pos.up()).isAir()) return false;
        if (!mc.world.getBlockState(pos.up(2)).isAir()) return false;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = pos.offset(dir);
            if (mc.world.getBlockState(neighbor).getBlock() != Blocks.BEDROCK &&
                mc.world.getBlockState(neighbor).getBlock() != Blocks.OBSIDIAN) return false;
        }
        return true;
    }

    /** Find the first hotbar slot containing a placeable block. */
    public static int findHotbarBlock() {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.BlockItem) {
                return i;
            }
        }
        return -1;
    }
}
