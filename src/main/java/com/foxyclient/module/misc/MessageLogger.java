package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.text.Text;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Logs all chat messages, whispers, and system messages.
 */
public class MessageLogger extends Module {
    private final BoolSetting logChat = addSetting(new BoolSetting("Chat", "Log chat messages", true));
    private final BoolSetting logWhispers = addSetting(new BoolSetting("Whispers", "Log whispers", true));
    private final BoolSetting logSystem = addSetting(new BoolSetting("System", "Log system messages", false));

    private final List<LogEntry> logs = new ArrayList<>();

    public MessageLogger() {
        super("MessageLogger", "Log all messages", Category.MISC);
    }

    public void onChatReceived(Text message) {
        if (!isEnabled()) return;
        String text = message.getString();

        // Categorize
        String type = "chat";
        if (text.contains("whispers to you") || text.contains("-> you")) type = "whisper";
        if (text.startsWith("[Server]") || text.startsWith("[")) type = "system";

        if (type.equals("chat") && !logChat.get()) return;
        if (type.equals("whisper") && !logWhispers.get()) return;
        if (type.equals("system") && !logSystem.get()) return;

        logs.add(new LogEntry(type, text, LocalDateTime.now()));

        // Keep log manageable
        if (logs.size() > 1000) logs.remove(0);
    }

    public List<LogEntry> getLogs() { return logs; }
    public void clearLogs() { logs.clear(); }

    public record LogEntry(String type, String message, LocalDateTime timestamp) {
        public String format() {
            return "[" + timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] [" + type + "] " + message;
        }
    }
}
