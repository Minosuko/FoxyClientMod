package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.pathfinding.PathFinder;
import net.minecraft.util.math.BlockPos;

public class GotoCommand extends Command {
    public GotoCommand() {
        super("goto", "Navigate to coordinates", ".goto <x> <y> <z>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 3) { error("Usage: " + getSyntax()); return; }
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            BlockPos target = new BlockPos(x, y, z);
            FoxyClient.INSTANCE.getPathFinder().pathTo(target);
            info("Pathing to §e" + x + " " + y + " " + z);
        } catch (NumberFormatException e) {
            error("Invalid coordinates!");
        }
    }
}
