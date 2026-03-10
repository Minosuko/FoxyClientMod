package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.foxyclient.seedcracker.config.Config;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class DatabaseCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedcracker")
            .then(ClientCommandManager.literal("database")
                .then(ClientCommandManager.literal("submit")
                    .executes(context -> {
                        Config.get().databaseSubmits = !Config.get().databaseSubmits;
                        context.getSource().sendFeedback(Text.literal("Database submits: " + Config.get().databaseSubmits));
                        return 1;
                    }))));
    }
}
