package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class CancelCommand extends Command {
    public CancelCommand() {
        super("cancel", "Cancel all tasks", ".cancel");
    }

    @Override
    public void execute(String[] args) {
        FoxyClient.INSTANCE.getPathFinder().cancelAll();
    }
}
