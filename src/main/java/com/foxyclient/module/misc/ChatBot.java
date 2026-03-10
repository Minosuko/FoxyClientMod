package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

import java.util.HashMap;
import java.util.Map;

/** ChatBot - Auto-responds to chat messages with configurable responses. */
public class ChatBot extends Module {
    private final BoolSetting greetings = addSetting(new BoolSetting("Greetings", "Auto greet players", true));
    private int cooldown = 0;

    private static final Map<String, String> RESPONSES = new HashMap<>();
    static {
        RESPONSES.put("hello", "Hey there!");
        RESPONSES.put("hi", "Hello!");
        RESPONSES.put("hey", "Hey!");
        RESPONSES.put("gg", "GG!");
    }

    public ChatBot() { super("ChatBot", "Auto respond to chat messages", Category.MISC); }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;
        if (cooldown > 0) return;
        if (event.getPacket() instanceof GameMessageS2CPacket pkt) {
            String msg = pkt.content().getString().toLowerCase();
            if (msg.contains(mc.player.getName().getString())) return; // Ignore own messages

            for (var entry : RESPONSES.entrySet()) {
                if (msg.contains(entry.getKey())) {
                    mc.player.networkHandler.sendChatMessage(entry.getValue());
                    cooldown = 100;
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTick(com.foxyclient.event.events.TickEvent event) {
        if (cooldown > 0) cooldown--;
    }
}
