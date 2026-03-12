package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.module.Module;
import org.lwjgl.glfw.GLFW;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Bind a key to a module", "bind <module> <key>");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) { error("Usage: " + getSyntax()); return; }
        Module module = FoxyClient.INSTANCE.getModuleManager().getModule(args[0]);
        if (module == null) { error("Module not found: " + args[0]); return; }

        String keyName = args[1].toUpperCase();
        int key = getKeyFromName(keyName);
        if (key == GLFW.GLFW_KEY_UNKNOWN && !keyName.equals("NONE")) {
            error("Unknown key: " + args[1]);
            return;
        }
        module.setKeybind(key);
        info("Bound " + module.getName() + " to §b" + GLFW.glfwGetKeyName(key, 0));
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

    private int getKeyFromName(String name) {
        if (name.equals("NONE")) return GLFW.GLFW_KEY_UNKNOWN;
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
            if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
        }
        return switch (name) {
            case "LSHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL" -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT" -> GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT" -> GLFW.GLFW_KEY_RIGHT_ALT;
            case "TAB" -> GLFW.GLFW_KEY_TAB;
            case "CAPSLOCK" -> GLFW.GLFW_KEY_CAPS_LOCK;
            case "SPACE" -> GLFW.GLFW_KEY_SPACE;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
