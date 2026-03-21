package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.pathfinding.PathFinder;
import net.minecraft.util.math.BlockPos;

public class GotoCommand extends Command {
    public GotoCommand() {
        super("goto", "Go to a location using Baritone", "goto <x y z|player>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 1) {
            // Goto player
            if (mc.world == null) { error("Not in a world!"); return; }
            String targetName = args[0];
            net.minecraft.entity.player.PlayerEntity target = null;
            for (net.minecraft.entity.player.PlayerEntity player : mc.world.getPlayers()) {
                if (player.getName().getString().equalsIgnoreCase(targetName)) {
                    target = player;
                    break;
                }
            }
            if (target == null) { error("Player not found: " + targetName); return; }
            FoxyClient.INSTANCE.getPathFinder().pathTo(target.getBlockPos());
            info("Pathing to §e" + target.getName().getString());
            return;
        }

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

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1 && mc.getNetworkHandler() != null) {
            return mc.getNetworkHandler().getPlayerList().stream()
                .map(entry -> entry.getProfile().name())
                .filter(name -> name != null && !name.equals(mc.player.getGameProfile().name()))
                .toList();
        }
        return super.getSuggestions(args);
    }
}
