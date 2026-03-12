package com.foxyclient.command;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.commands.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all commands and dispatches them from chat input.
 */
public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public CommandManager() {
        register(new ToggleCommand());
        register(new BindCommand());
        register(new PrefixCommand());
        register(new HelpCommand());
        register(new SeedCommand());
        register(new ConfigCommand());
        register(new GotoCommand());
        register(new MineCommand());
        register(new FollowCommand());
        register(new StopCommand());
        register(new FriendCommand());
        register(new WaypointCommand());
        register(new DupeCommand());
        register(new EnderChestCommand());

        // Baritone commands
        register(new TunnelCommand());
        register(new StripMineCommand());
        register(new FarmCommand());
        register(new ChopCommand());
        register(new BuildCommand());
        register(new ClearCommand());
        register(new PauseCommand());
        register(new ResumeCommand());
        register(new CancelCommand());
        register(new GoalCommand());
        register(new PathCommand());
    }

    private void register(Command command) {
        commands.add(command);
    }

    public boolean handleChat(String message) {
        // Support both . and # as command prefixes
        boolean isCmd = message.startsWith(prefix);
        boolean isHash = message.startsWith("#");
        if (!isCmd && !isHash) return false;

        String input = isHash ? message.substring(1) : message.substring(prefix.length());
        String[] parts = input.split("\\s+");
        if (parts.length == 0) return true;

        String cmdName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        for (Command cmd : commands) {
            if (cmd.getName().equalsIgnoreCase(cmdName)) {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    cmd.error("Error: " + e.getMessage());
                }
                return true;
            }
        }

        if (mc().player != null) {
            mc().player.sendMessage(
                net.minecraft.text.Text.literal("§7[§6FoxyClient§7] §cUnknown command: " + cmdName), false
            );
        }
        return true;
    }

    public List<String> getSuggestions(String message) {
        boolean isCmd = message.startsWith(prefix);
        boolean isHash = message.startsWith("#");
        if (!isCmd && !isHash) return List.of();

        String input = isHash ? message.substring(1) : message.substring(prefix.length());
        String[] parts = input.split("\\s+", -1);
        String current = parts[parts.length - 1].toLowerCase();

        List<String> suggestions = new ArrayList<>();

        if (parts.length == 1) {
            // Suggest command names
            for (Command cmd : commands) {
                if (cmd.getName().toLowerCase().startsWith(current)) {
                    suggestions.add((isHash ? "#" : prefix) + cmd.getName());
                }
            }
        } else {
            // Suggest command arguments
            String cmdName = parts[0];
            for (Command cmd : commands) {
                if (cmd.getName().equalsIgnoreCase(cmdName)) {
                    String[] args = new String[parts.length - 1];
                    System.arraycopy(parts, 1, args, 0, args.length);
                    for (String s : cmd.getSuggestions(args)) {
                        if (s.toLowerCase().startsWith(current)) {
                            suggestions.add(s);
                        }
                    }
                    break;
                }
            }
        }

        return suggestions;
    }

    private net.minecraft.client.MinecraftClient mc() {
        return net.minecraft.client.MinecraftClient.getInstance();
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public List<Command> getCommands() { return commands; }
}
