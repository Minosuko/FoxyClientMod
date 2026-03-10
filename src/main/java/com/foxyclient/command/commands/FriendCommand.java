package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Manage friends list", ".friend <add|remove|list> [name]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }

        var friends = FoxyClient.INSTANCE.getFriendsManager();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { error("Specify player name!"); return; }
                friends.addFriend(args[1]);
                info("§aAdded §e" + args[1] + " §ato friends list!");
            }
            case "remove", "del" -> {
                if (args.length < 2) { error("Specify player name!"); return; }
                friends.removeFriend(args[1]);
                info("§cRemoved §e" + args[1] + " §cfrom friends list.");
            }
            case "list" -> {
                var list = friends.getFriends();
                if (list.isEmpty()) { info("§7No friends added."); return; }
                info("§6Friends (§e" + list.size() + "§6):");
                for (String name : list) info("  §7- §f" + name);
            }
            default -> error("Unknown subcommand: " + args[0]);
        }
    }

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return java.util.List.of("add", "remove", "list", "clear");
        }
        return super.getSuggestions(args);
    }
}
