package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.command.Command;

public class FarmCommand extends Command {
    public FarmCommand() {
        super("farm", "Auto farm crops", "farm [radius]");
    }

    @Override
    public void execute(String[] args) {
        String cmd = "farm";
        if (args.length > 0) cmd += " " + args[0];
        
        com.foxybot.api.FoxyBotAPI.getProvider().getPrimaryFoxyBot().getCommandManager().execute(cmd);
        info("Executing Baritone: §e" + cmd);
    }
}
