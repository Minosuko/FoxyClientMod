package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.foxyclient.seedcracker.FoxySeedCracker;
import com.foxyclient.seedcracker.util.Log;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public class DataCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedcracker")
            .then(ClientCommandManager.literal("data")
                .then(ClientCommandManager.literal("clear")
                    .executes(context -> {
                        FoxySeedCracker.get().reset();
                        context.getSource().sendFeedback(Text.literal("Data cleared"));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("bits")
                    .executes(context -> {
                        double bits = FoxySeedCracker.get().getDataStorage().getBaseBits();
                        context.getSource().sendFeedback(Text.literal("Collected bits: " + bits));
                        return 1;
                    }))));
    }
}
