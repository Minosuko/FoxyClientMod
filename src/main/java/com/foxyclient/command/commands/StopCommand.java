package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop", "Stop current Baritone task", "stop");
    }

    @Override
    public void execute(String[] args) {
        FoxyClient.INSTANCE.getPathFinder().cancelAll();
        info("Pathfinding §cstopped§f.");
    }
}
