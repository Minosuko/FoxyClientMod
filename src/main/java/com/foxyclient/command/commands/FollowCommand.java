package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import net.minecraft.entity.player.PlayerEntity;

public class FollowCommand extends Command {
    public FollowCommand() {
        super("follow", "Follow a player", ".follow <player>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }
        if (mc.world == null) { error("Not in a world!"); return; }

        String targetName = args[0];
        PlayerEntity target = null;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(targetName)) {
                target = player;
                break;
            }
        }

        if (target == null) { error("Player not found: " + targetName); return; }
        FoxyClient.INSTANCE.getPathFinder().follow(target);
        info("Following §e" + target.getName().getString());
    }
}
