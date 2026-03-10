package com.foxyclient.module.world;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Automatically builds nether highways (flat obsidian paths).
 */
public class HighwayBuilder extends Module {
    private final ModeSetting direction = addSetting(new ModeSetting("Direction", "Build direction", "North",
        "North", "South", "East", "West"));
    private final NumberSetting width = addSetting(new NumberSetting("Width", "Highway width", 3, 1, 5));

    public HighwayBuilder() {
        super("HighwayBuilder", "Auto build nether highways", Category.WORLD);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        int obsSlot = findSlot(Items.OBSIDIAN);
        if (obsSlot == -1) { error("No obsidian!"); return; }

        Direction dir = switch (direction.get()) {
            case "North" -> Direction.NORTH;
            case "South" -> Direction.SOUTH;
            case "East" -> Direction.EAST;
            case "West" -> Direction.WEST;
            default -> Direction.NORTH;
        };

        BlockPos playerPos = mc.player.getBlockPos();
        int w = width.get().intValue();

        // Mine blocks ahead
        for (int i = 1; i <= 3; i++) {
            BlockPos ahead = playerPos.offset(dir, i);
            for (int dy = 0; dy <= 2; dy++) {
                BlockPos minePos = ahead.up(dy);
                if (!mc.world.getBlockState(minePos).isAir()) {
                    mc.interactionManager.attackBlock(minePos, dir);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }

        // Place floor
        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = obsSlot;

        for (int i = 0; i <= 2; i++) {
            BlockPos floorPos = playerPos.offset(dir, i).down();
            for (int side = -(w / 2); side <= w / 2; side++) {
                BlockPos placePos = switch (dir) {
                    case NORTH, SOUTH -> floorPos.add(side, 0, 0);
                    case EAST, WEST -> floorPos.add(0, 0, side);
                    default -> floorPos;
                };
                if (mc.world.getBlockState(placePos).isAir()) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                        new BlockHitResult(Vec3d.ofCenter(placePos), Direction.UP, placePos, false));
                }
            }
        }

        mc.player.getInventory().selectedSlot = oldSlot;
    }

    private int findSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) return i;
        }
        return -1;
    }
}
