package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * Automatically sends chat messages at intervals.
 */
public class Spam extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Message delay (ticks)", 100, 20, 1200));
    private final BoolSetting random = addSetting(new BoolSetting("Random", "Randomize message order", false));

    private final List<String> messages = new ArrayList<>();
    private int timer = 0;
    private int messageIndex = 0;

    public Spam() {
        super("Spam", "Auto send chat messages", Category.MISC);
        messages.add("FoxyClient on top!");
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (messages.isEmpty()) return;

        timer++;
        if (timer < delay.get()) return;
        timer = 0;

        int index = random.get() ? (int) (Math.random() * messages.size()) : messageIndex;
        String msg = messages.get(index % messages.size());

        mc.player.networkHandler.sendChatMessage(msg);
        messageIndex = (messageIndex + 1) % messages.size();
    }

    public List<String> getMessages() { return messages; }
}
