package com.foxybot.api.pathing.goals;

import net.minecraft.util.math.BlockPos;

public class GoalBlock implements Goal {
    private final BlockPos pos;

    public GoalBlock(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getGoalPos() {
        return pos;
    }
}
