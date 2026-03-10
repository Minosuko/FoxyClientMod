package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.seedfinding.mccore.version.MCVersion;
import com.foxyclient.seedcracker.config.Config;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class VersionCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedcracker")
            .then(ClientCommandManager.literal("version")
                .then(ClientCommandManager.argument("version", StringArgumentType.string())
                    .executes(context -> {
                        String vName = StringArgumentType.getString(context, "version");
                        MCVersion version = MCVersion.fromString(vName);
                        if (version != null) {
                            Config.get().setVersion(version);
                            context.getSource().sendFeedback(Text.literal("Version set to " + version.name));
                        } else {
                            context.getSource().sendError(Text.literal("Invalid version: " + vName));
                        }
                        return 1;
                    }))));
    }
}
