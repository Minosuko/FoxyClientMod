package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.module.Module;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggle a module on/off", ".toggle <module>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) { error("Usage: " + getSyntax()); return; }
        Module module = FoxyClient.INSTANCE.getModuleManager().getModule(args[0]);
        if (module == null) { error("Module not found: " + args[0]); return; }
        module.toggle();
        info(module.getName() + " " + (module.isEnabled() ? "§aenabled" : "§cdisabled"));
    }

    @Override
    public java.util.List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            return FoxyClient.INSTANCE.getModuleManager().getModules().stream()
                .map(Module::getName)
                .toList();
        }
        return super.getSuggestions(args);
    }
}
