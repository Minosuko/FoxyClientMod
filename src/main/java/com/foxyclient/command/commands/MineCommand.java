package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class MineCommand extends Command {
    public MineCommand() {
        super("mine", "Mine blocks using Baritone", "mine <block>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }
        String block = args[0].toLowerCase();
        if (!block.contains(":")) block = "minecraft:" + block;
        FoxyClient.INSTANCE.getPathFinder().mine(block);
        info("Mining §e" + block);
    }

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return java.util.List.of(
                "diamond_ore", "deepslate_diamond_ore",
                "iron_ore", "deepslate_iron_ore",
                "gold_ore", "deepslate_gold_ore",
                "coal_ore", "deepslate_coal_ore",
                "emerald_ore", "deepslate_emerald_ore",
                "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore",
                "copper_ore", "deepslate_copper_ore",
                "nether_quartz_ore", "nether_gold_ore",
                "ancient_debris"
            );
        }
        return super.getSuggestions(args);
    }
}
