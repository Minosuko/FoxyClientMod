package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
// import com.foxyclient.pathfinding.tasks.ClearTask;

public class ClearCommand extends Command {
    public ClearCommand() {
        super("clear", "Clear an area using Baritone", "clear <radius>");
    }

    @Override
    public void execute(String[] args) {
        int radius = 5;
        if (args.length > 0) try { radius = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        // FoxyClient.INSTANCE.getPathFinder().submitTask(new ClearTask(mc.player.getBlockPos(), radius));
        info("Clearing " + radius + " block radius (NOT IMPLEMENTED).");
        info("Clearing " + radius + " block radius.");
    }
    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) return java.util.List.of("5", "10", "20", "50");
        return super.getSuggestions(args);
    }
}
