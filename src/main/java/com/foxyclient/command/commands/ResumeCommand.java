package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class ResumeCommand extends Command {
    public ResumeCommand() {
        super("resume", "Resume paused Baritone task", "resume");
    }

    @Override
    public void execute(String[] args) {
        FoxyClient.INSTANCE.getPathFinder().resume();
    }
}
