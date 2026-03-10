package com.foxyclient.module.ui;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.KeyEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.KeySetting;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import org.lwjgl.glfw.GLFW;

/**
 * Allows binding custom actions to key combinations (macro system).
 */
public class CustomKeybinds extends Module {
    private final KeySetting slot1Key = addSetting(new KeySetting("Slot1Key", "Keybind for action 1 (Toggle last used module)", GLFW.GLFW_KEY_UNKNOWN));
    private final KeySetting slot2Key = addSetting(new KeySetting("Slot2Key", "Keybind for action 2 (Panic disconnect)", GLFW.GLFW_KEY_UNKNOWN));

    public CustomKeybinds() {
        super("CustomKeybinds", "Custom key action bindings", Category.UI);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (nullCheck()) return;
        if (mc.currentScreen != null) return;
        if (event.getAction() != 1) return; // Only trigger on press

        int key = event.getKey();

        if (slot1Key.get() != GLFW.GLFW_KEY_UNKNOWN && key == slot1Key.get()) {
            Module lastToggled = FoxyClient.INSTANCE.getModuleManager().getLastToggledModule();
            if (lastToggled != null) {
                lastToggled.toggle();
                info("Toggled module: " + lastToggled.getName());
            } else {
                error("No recently toggled module found.");
            }
        }

        if (slot2Key.get() != GLFW.GLFW_KEY_UNKNOWN && key == slot2Key.get()) {
            if (mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().getConnection().disconnect(net.minecraft.text.Text.literal("[CustomKeybinds] Panic disconnect triggered"));
            }
        }
    }
}
