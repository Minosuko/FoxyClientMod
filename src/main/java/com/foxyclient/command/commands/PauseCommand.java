package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class PauseCommand extends Command {
    public PauseCommand() {
        super("pause", "Pause current Baritone task", "pause");
    }

    @Override
    public void execute(String[] args) {
        FoxyClient.INSTANCE.getPathFinder().pause();
    }
}
