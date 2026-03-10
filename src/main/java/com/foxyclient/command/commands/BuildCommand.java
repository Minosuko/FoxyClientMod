package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.command.Command;

public class BuildCommand extends Command {
    public BuildCommand() {
        super("build", "Build a structure", ".build <sizeX> <sizeY> <sizeZ>");
    }

    @Override
    public void execute(String[] args) {
        info("§cNote: Baritone building requires a schematic file.");
        info("Use Baritone's native command: §7#build <filename>");
    }
}
