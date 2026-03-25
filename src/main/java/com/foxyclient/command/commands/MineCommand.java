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
            return net.minecraft.registry.Registries.BLOCK.getIds().stream()
                .map(net.minecraft.util.Identifier::getPath)
                .collect(java.util.stream.Collectors.toList());
        }
        return super.getSuggestions(args);
    }
}
