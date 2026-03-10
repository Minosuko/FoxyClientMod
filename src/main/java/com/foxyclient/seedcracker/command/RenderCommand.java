package com.foxyclient.seedcracker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.foxyclient.seedcracker.config.Config;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;

public class RenderCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(ClientCommandManager.literal("seedcracker")
            .then(ClientCommandManager.literal("render")
                .then(ClientCommandManager.literal("off")
                    .executes(context -> {
                        Config.get().render = Config.RenderType.OFF;
                        context.getSource().sendFeedback(Text.literal("Render set to OFF"));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("on")
                    .executes(context -> {
                        Config.get().render = Config.RenderType.ON;
                        context.getSource().sendFeedback(Text.literal("Render set to ON"));
                        return 1;
                    }))
                .then(ClientCommandManager.literal("xray")
                    .executes(context -> {
                        Config.get().render = Config.RenderType.XRAY;
                        context.getSource().sendFeedback(Text.literal("Render set to XRAY"));
                        return 1;
                    }))));
    }
}
