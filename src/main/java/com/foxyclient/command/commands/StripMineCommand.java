package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class StripMineCommand extends Command {
    public StripMineCommand() {
        super("stripmine", "Start strip mining using Baritone", "stripmine [y] [branchLen] [spacing]");
    }

    @Override
    public void execute(String[] args) {
        String cmd = "strip";
        if (args.length > 0) cmd += " " + args[0];
        if (args.length > 1) cmd += " " + args[1];
        if (args.length > 2) cmd += " " + args[2];
        
        com.foxybot.api.FoxyBotAPI.getProvider().getPrimaryFoxyBot().getCommandManager().execute(cmd);
        info("Executing Baritone: §e" + cmd);
    }
}
