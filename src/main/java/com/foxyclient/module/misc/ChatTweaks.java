package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

/**
 * ChatTweaks - Chat improvements including timestamps, infinite history, and anti-spam.
 * Intercepts incoming chat messages to add timestamps and detect duplicate messages.
 */
public class ChatTweaks extends Module {
    private final BoolSetting timestamps = addSetting(new BoolSetting("Timestamps", "Show message timestamps", true));
    private final BoolSetting infiniteHistory = addSetting(new BoolSetting("InfiniteHistory", "Unlimited chat history", true));
    private final BoolSetting antiSpam = addSetting(new BoolSetting("AntiSpam", "Group repeat messages", true));
    private final BoolSetting copyOnClick = addSetting(new BoolSetting("CopyClick", "Click to copy messages", true));
    private final ModeSetting format = addSetting(new ModeSetting("TimeFormat", "Time format", "HH:mm", "HH:mm", "hh:mm a", "HH:mm:ss"));

    private final LinkedList<String> recentMessages = new LinkedList<>();
    private static final int SPAM_HISTORY = 10;

    public ChatTweaks() {
        super("ChatTweaks", "Chat improvements", Category.MISC);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String content = packet.content().getString();

            // Anti-spam: check for duplicate messages
            if (antiSpam.get()) {
                int dupeCount = 0;
                for (String msg : recentMessages) {
                    if (msg.equals(content)) dupeCount++;
                }
                if (dupeCount >= 2) {
                    event.cancel();
                    return;
                }
                recentMessages.addLast(content);
                if (recentMessages.size() > SPAM_HISTORY) {
                    recentMessages.removeFirst();
                }
            }

            // Timestamps: prepend time to message
            if (timestamps.get()) {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format.get());
                String time = LocalTime.now().format(dtf);
                MutableText timeText = Text.literal("§7[" + time + "] §r");
                MutableText newContent = timeText.append(packet.content().copy());

                event.cancel();
                mc.player.sendMessage(newContent, packet.overlay());
            }
        }
    }

    @Override
    public void onDisable() {
        recentMessages.clear();
    }

    public boolean showTimestamps() { return isEnabled() && timestamps.get(); }
    public boolean isInfiniteHistory() { return isEnabled() && infiniteHistory.get(); }
    public boolean isAntiSpam() { return isEnabled() && antiSpam.get(); }
    public String getFormat() { return format.get(); }
}
