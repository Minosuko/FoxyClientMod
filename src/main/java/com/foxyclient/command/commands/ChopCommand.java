package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.command.Command;

public class ChopCommand extends Command {
    public ChopCommand() {
        super("chop", "Chop nearby trees using Baritone", "chop [radius]");
    }

    @Override
    public void execute(String[] args) {
        String cmd = "mine log";
        com.foxybot.api.FoxyBotAPI.getProvider().getPrimaryFoxyBot().getCommandManager().execute(cmd);
        info("Executing Baritone: §e" + cmd);
    }
    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) return java.util.List.of("10", "20", "50", "100");
        return super.getSuggestions(args);
    }
}
