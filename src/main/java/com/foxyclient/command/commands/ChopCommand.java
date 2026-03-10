package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.command.Command;

public class ChopCommand extends Command {
    public ChopCommand() {
        super("chop", "Auto chop trees", ".chop [radius]");
    }

    @Override
    public void execute(String[] args) {
        String cmd = "mine log";
        com.foxybot.api.FoxyBotAPI.getProvider().getPrimaryFoxyBot().getCommandManager().execute(cmd);
        info("Executing Baritone: §e" + cmd);
    }
}
