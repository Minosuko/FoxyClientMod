package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.util.WaypointManager;

public class WaypointCommand extends Command {
    public WaypointCommand() {
        super("waypoint", "Manage waypoints", "waypoint <add|remove|list|goto> [name] [x y z]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }

        var manager = FoxyClient.INSTANCE.getWaypointManager();

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) { error("Specify waypoint name!"); return; }
                String name = args[1];
                int x, y, z;
                String dim = mc.world != null ? mc.world.getRegistryKey().getValue().toString() : "unknown";

                if (args.length >= 5) {
                    try {
                        x = Integer.parseInt(args[2]); y = Integer.parseInt(args[3]); z = Integer.parseInt(args[4]);
                    } catch (NumberFormatException e) { error("Invalid coordinates!"); return; }
                } else if (mc.player != null) {
                    x = mc.player.getBlockX(); y = mc.player.getBlockY(); z = mc.player.getBlockZ();
                } else {
                    error("Not in a world!"); return;
                }

                manager.add(name, x, y, z, dim);
                info("§aWaypoint §e" + name + " §aset at §f" + x + " " + y + " " + z);
            }
            case "remove", "del" -> {
                if (args.length < 2) { error("Specify waypoint name!"); return; }
                if (manager.remove(args[1])) info("§cRemoved waypoint §e" + args[1]);
                else error("Waypoint not found: " + args[1]);
            }
            case "list" -> {
                var all = manager.getAll();
                if (all.isEmpty()) { info("§7No waypoints."); return; }
                info("§6Waypoints (§e" + all.size() + "§6):");
                for (var wp : all) {
                    info("  §7- §f" + wp.name() + " §7[" + wp.x() + " " + wp.y() + " " + wp.z() + "] §8" + wp.dimension());
                }
            }
            case "goto" -> {
                if (args.length < 2) { error("Specify waypoint name!"); return; }
                var wp = manager.get(args[1]);
                if (wp == null) { error("Waypoint not found: " + args[1]); return; }
                FoxyClient.INSTANCE.getPathFinder().pathTo(new net.minecraft.util.math.BlockPos(wp.x(), wp.y(), wp.z()));
                info("§aPathing to waypoint §e" + wp.name());
            }
            default -> error("Usage: " + getSyntax());
        }
    }

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return java.util.List.of("add", "remove", "list", "goto");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("goto") || args[0].equalsIgnoreCase("del"))) {
            return FoxyClient.INSTANCE.getWaypointManager().getAll().stream()
                .map(com.foxyclient.util.WaypointManager.Waypoint::name)
                .toList();
        }
        return super.getSuggestions(args);
    }
}
