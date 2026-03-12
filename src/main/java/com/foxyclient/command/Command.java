package com.foxyclient.command;

import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * Base command class.
 */
public abstract class Command {
    private final String name;
    private final String description;
    private final String syntax;
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public Command(String name, String description, String syntax) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
    }

    public abstract void execute(String[] args);

    public List<String> getSuggestions(String[] args) {
        return List.of();
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSyntax() { 
        return com.foxyclient.FoxyClient.INSTANCE.getCommandManager().getPrefix() + syntax; 
    }

    protected void info(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[§6FoxyClient§7] §f" + message), false
            );
        }
    }

    protected void error(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(
                net.minecraft.text.Text.literal("§7[§6FoxyClient§7] §c" + message), false
            );
        }
    }
}
