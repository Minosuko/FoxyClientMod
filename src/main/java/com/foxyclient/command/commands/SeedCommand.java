package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.module.render.OreSim;

public class SeedCommand extends Command {
    public SeedCommand() {
        super("seed", "Set world seed for OreSim", ".seed <seed>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            error("Usage: .seed <set|crack> [value]");
            return;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("set")) {
            if (args.length < 2) {
                error("Usage: .seed set <seed>");
                return;
            }
            try {
                long seed = Long.parseLong(args[1]);
                updateSeed(seed);
            } catch (NumberFormatException e) {
                error("Invalid seed! Must be a long number.");
            }
        } else if (sub.equals("crack")) {
            info("Starting seed cracking...");
            // Trigger cracking logic if available
            com.foxyclient.seedcracker.FoxySeedCracker.get().getDataStorage().getTimeMachine().poke(com.foxyclient.seedcracker.cracker.storage.TimeMachine.Phase.BIOMES);
        } else if (sub.equals("gui")) {
            info("Opening SeedCracker GUI...");
            // Use the built-in GUI opening flag
            com.foxyclient.seedcracker.FoxySeedCracker.get().getDataStorage().openGui = true;
        } else {
            // Fallback for old .seed <seed> usage
            try {
                long seed = Long.parseLong(args[0]);
                updateSeed(seed);
            } catch (NumberFormatException e) {
                error("Usage: .seed <set|crack> [value]");
            }
        }
    }

    private void updateSeed(long seed) {
        var oreSim = (OreSim) FoxyClient.INSTANCE.getModuleManager().getModule("OreSim");
        if (oreSim != null) {
            oreSim.updateSeed(seed);
            info("OreSim seed set to: §a" + seed);
        } else {
            error("OreSim module not found!");
        }
    }

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return java.util.List.of("set", "crack", "gui", "copy");
        }
        return super.getSuggestions(args);
    }
}
