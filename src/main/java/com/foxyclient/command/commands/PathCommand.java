package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.pathfinding.PathFinder;
import net.minecraft.util.math.BlockPos;

public class PathCommand extends Command {
    public PathCommand() {
        super("path", "Show pathfinding status", ".path");
    }

    @Override
    public void execute(String[] args) {
        var pf = FoxyClient.INSTANCE.getPathFinder();
        info("§eBaritone Status:");
        info("  Active: " + pf.isActive());
        info("  Paused: " + pf.isPaused());
        info("  Process: §a" + pf.getCurrentProcessName());

        BlockPos goal = pf.getGoalPos();
        if (goal != null) {
            info("  Goal: §b" + goal.getX() + ", " + goal.getY() + ", " + goal.getZ());
        }
    }
}
