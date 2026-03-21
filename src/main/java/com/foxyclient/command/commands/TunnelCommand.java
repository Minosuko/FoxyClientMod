package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class TunnelCommand extends Command {
    public TunnelCommand() {
        super("tunnel", "Start tunneling using Baritone", "tunnel [length]");
    }

    @Override
    public void execute(String[] args) {
        String cmd = "tunnel";
        if (args.length > 0) cmd += " " + args[0];
        
        com.foxybot.api.FoxyBotAPI.getProvider().getPrimaryFoxyBot().getCommandManager().execute(cmd);
        info("Executing Baritone: §e" + cmd);
    }
    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) return java.util.List.of("10", "20", "50", "100", "500", "1000");
        return super.getSuggestions(args);
    }
}
