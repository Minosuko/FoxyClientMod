package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

/**
 * BetterChat - Various chat improvements.
 * Provides no-background option, infinite history, timestamps, and anti-spam.
 * Listens for incoming game messages and modifies them as needed.
 */
public class BetterChat extends Module {
    private final BoolSetting noBackground = addSetting(new BoolSetting("NoBackground", "Remove chat background", false));
    private final BoolSetting infiniteHistory = addSetting(new BoolSetting("InfiniteHistory", "Infinite chat history", true));
    private final BoolSetting timestamps = addSetting(new BoolSetting("Timestamps", "Show timestamps", true));
    private final BoolSetting antiSpam = addSetting(new BoolSetting("AntiSpam", "Stack duplicate messages", true));

    private final LinkedList<String> recentMessages = new LinkedList<>();
    private final LinkedList<Integer> messageCounts = new LinkedList<>();
    private static final int MAX_TRACKED = 20;

    public BetterChat() {
        super("BetterChat", "Chat improvements", Category.MISC);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String content = packet.content().getString();

            // Anti-spam: detect and count duplicates
            if (antiSpam.get()) {
                int existingIndex = -1;
                for (int i = 0; i < recentMessages.size(); i++) {
                    if (recentMessages.get(i).equals(content)) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex >= 0) {
                    int count = messageCounts.get(existingIndex) + 1;
                    messageCounts.set(existingIndex, count);

                    // Cancel original and resend with count
                    event.cancel();
                    MutableText newContent = packet.content().copy().append(
                        Text.literal(" §7(x" + count + ")")
                    );
                    if (timestamps.get()) {
                        newContent = prependTimestamp(newContent);
                    }
                    mc.player.sendMessage(newContent, packet.overlay());
                    return;
                }

                recentMessages.addLast(content);
                messageCounts.addLast(1);
                if (recentMessages.size() > MAX_TRACKED) {
                    recentMessages.removeFirst();
                    messageCounts.removeFirst();
                }
            }

            // Timestamps
            if (timestamps.get()) {
                event.cancel();
                MutableText newContent = prependTimestamp(packet.content().copy());
                mc.player.sendMessage(newContent, packet.overlay());
            }
        }
    }

    private MutableText prependTimestamp(MutableText content) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String time = LocalTime.now().format(dtf);
        return Text.literal("§8[§7" + time + "§8] §r").append(content);
    }

    @Override
    public void onDisable() {
        recentMessages.clear();
        messageCounts.clear();
    }

    public boolean hasNoBackground() { return noBackground.get() && isEnabled(); }
    public boolean hasTimestamps() { return timestamps.get() && isEnabled(); }
    public boolean hasAntiSpam() { return antiSpam.get() && isEnabled(); }
    public boolean hasInfiniteHistory() { return infiniteHistory.get() && isEnabled(); }
}
