package com.foxyclient.command.commands;

import com.foxyclient.command.Command;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;

public class EnderChestCommand extends Command {
    public EnderChestCommand() {
        super("enderchest", "Open your ender chest", "enderchest");
    }

    @Override
    public void execute(String[] args) {
        if (mc.player == null) return;
        
        mc.player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
            (syncId, inventory, player) -> GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, mc.player.getEnderChestInventory()),
            Text.translatable("container.enderchest")
        ));
    }
}
