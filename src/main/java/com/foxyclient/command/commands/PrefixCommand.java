package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;

public class PrefixCommand extends Command {
    public PrefixCommand() {
        super("prefix", "Change command prefix", "prefix <char>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }
        FoxyClient.INSTANCE.getCommandManager().setPrefix(args[0]);
        info("Prefix set to: §6" + args[0]);
    }
    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) return java.util.List.of(".", "#", "!", ",", "-");
        return super.getSuggestions(args);
    }
}
