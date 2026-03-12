package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("config", "Save or load configuration", "config <save|load> [name]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }
        switch (args[0].toLowerCase()) {
            case "save" -> {
                FoxyClient.INSTANCE.getModuleManager().saveConfig();
                info("Config §asaved§f!");
            }
            case "load" -> {
                FoxyClient.INSTANCE.getModuleManager().loadConfig();
                info("Config §aloaded§f!");
            }
            default -> error("Usage: " + getSyntax());
        }
    }
}
