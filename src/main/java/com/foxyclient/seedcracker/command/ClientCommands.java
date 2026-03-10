package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.foxyclient.seedcracker.FoxySeedCracker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class ClientCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        CrackerCommand.register(dispatcher);
        RenderCommand.register(dispatcher);
        DataCommand.register(dispatcher);
        VersionCommand.register(dispatcher);
        DatabaseCommand.register(dispatcher);
    }
}
