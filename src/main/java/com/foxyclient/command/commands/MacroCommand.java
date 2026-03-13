package com.foxyclient.command.commands;

import com.foxyclient.FoxyClient;
import com.foxyclient.command.Command;
import com.foxyclient.module.ui.CustomKeybinds;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "Manage custom keybind macros", "macro <add|remove|list> <key> [command...]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            error("Usage: " + getSyntax());
            return;
        }

        CustomKeybinds customKeybinds = FoxyClient.INSTANCE.getModuleManager().getModule(CustomKeybinds.class);
        if (customKeybinds == null) {
            error("CustomKeybinds module not found!");
            return;
        }

        String action = args[0].toLowerCase();

        if (action.equals("list")) {
            Map<Integer, String> macros = customKeybinds.getMacros();
            if (macros.isEmpty()) {
                info("No macros defined.");
            } else {
                info("§e--- Macros ---");
                for (Map.Entry<Integer, String> entry : macros.entrySet()) {
                    String keyName = GLFW.glfwGetKeyName(entry.getKey(), 0);
                    if (keyName == null) keyName = "KEY_" + entry.getKey();
                    info("§b" + keyName.toUpperCase() + "§7 -> §f" + entry.getValue());
                }
            }
            return;
        }

        if (args.length < 2) {
            error("Usage: " + getSyntax());
            return;
        }

        String keyName = args[1].toUpperCase();
        int key = getKeyFromName(keyName);
        if (key == GLFW.GLFW_KEY_UNKNOWN) {
            error("Unknown key: " + args[1]);
            return;
        }

        if (action.equals("remove")) {
            customKeybinds.removeMacro(key);
            info("Removed macro for key §b" + keyName);
            return;
        }

        if (action.equals("add")) {
            if (args.length < 3) {
                error("Please provide a command for the macro.");
                return;
            }
            
            StringBuilder commandBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                commandBuilder.append(args[i]).append(" ");
            }
            String command = commandBuilder.toString().trim();
            
            customKeybinds.addMacro(key, command);
            info("Bound key §b" + keyName + "§7 to: §f" + command);
            return;
        }

        error("Unknown action: " + action + ". Use add, remove, or list.");
    }

    @Override
    public List<String> getSuggestions(String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("add");
            suggestions.add("remove");
            suggestions.add("list");
            return suggestions;
        }
        return super.getSuggestions(args);
    }

    private int getKeyFromName(String name) {
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
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }
}
