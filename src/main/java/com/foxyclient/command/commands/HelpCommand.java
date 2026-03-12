package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "List all commands", "help");
    }

    @Override
    public void execute(String[] args) {
        info("§6=== FoxyClient Commands ===");
        for (Command cmd : FoxyClient.INSTANCE.getCommandManager().getCommands()) {
            info("§e" + cmd.getSyntax() + " §7- " + cmd.getDescription());
        }
    }
}
