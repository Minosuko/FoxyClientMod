package com.foxyclient.command.commands;

import com.foxyclient.command.Command;

public class SayCommand extends Command {
    public SayCommand() {
        super("say", "Send a message to public chat", "say <message>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { 
            error("Usage: " + getSyntax()); 
            return; 
        }
        
        String message = String.join(" ", args);
        if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }
}
