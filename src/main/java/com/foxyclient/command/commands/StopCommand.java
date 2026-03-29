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
        
        com.foxyclient.module.world.FoxyBot foxyBot = FoxyClient.INSTANCE.getModuleManager().getModule(com.foxyclient.module.world.FoxyBot.class);
        if (foxyBot != null && foxyBot.isEnabled()) {
            foxyBot.setEnabled(false);
        }
        
        info("Pathfinding §cstopped§f.");
    }
}
