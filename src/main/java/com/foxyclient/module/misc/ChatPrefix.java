package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;

/**
 * ChatPrefix - Adds a configurable prefix and/or suffix to outgoing chat messages.
 * Intercepts outgoing ChatMessageC2SPacket and modifies the message content.
 */
public class ChatPrefix extends Module {
    private final BoolSetting prefixEnabled = addSetting(new BoolSetting("Prefix", "Enable prefix", true));
    private final BoolSetting suffixEnabled = addSetting(new BoolSetting("Suffix", "Enable suffix", false));

    private String prefix = "» ";
    private String suffix = " «";

    public ChatPrefix() {
        super("ChatPrefix", "Add prefix to messages", Category.MISC);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof ChatMessageC2SPacket packet) {
            String original = packet.chatMessage();

            // Don't modify commands
            if (original.startsWith("/")) return;

            StringBuilder modified = new StringBuilder();
            if (prefixEnabled.get() && !original.startsWith(prefix)) modified.append(prefix);
            modified.append(original);
            if (suffixEnabled.get() && !original.endsWith(suffix)) modified.append(suffix);

            String newMsg = modified.toString();
            if (!newMsg.equals(original)) {
                event.cancel();
                mc.player.networkHandler.sendChatMessage(newMsg);
            }
        }
    }

    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getPrefix() { return prefix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public String getSuffix() { return suffix; }
}
