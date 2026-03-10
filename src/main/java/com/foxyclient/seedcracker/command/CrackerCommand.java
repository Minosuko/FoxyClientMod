package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.util.Log;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class CrackerCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedcracker")
            .then(ClientCommandManager.literal("reset")
                .executes(context -> {
                    FoxySeedCracker.get().reset();
                    context.getSource().sendFeedback(Text.translatable("data.clearData"));
                    return 1;
                }))
            .then(ClientCommandManager.literal("status")
                .executes(context -> {
                    // Print status logic
                    return 1;
                })));
    }
}
