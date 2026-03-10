package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import net.minecraft.util.math.BlockPos;

public class GoalCommand extends Command {
    public GoalCommand() {
        super("goal", "Set or show goal", ".goal [x y z]");
    }

    @Override
    public void execute(String[] args) {
        var pf = FoxyClient.INSTANCE.getPathFinder();
        if (args.length >= 3) {
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                pf.setGoal(new BlockPos(x, y, z));
                info("Goal set to " + x + ", " + y + ", " + z);
            } catch (NumberFormatException e) {
                error("Invalid coordinates.");
            }
        } else {
            BlockPos goal = pf.getGoalPos();
            if (goal != null) {
                info("Current goal: " + goal.getX() + ", " + goal.getY() + ", " + goal.getZ());
            } else {
                info("No goal set.");
            }
        }
    }
}
