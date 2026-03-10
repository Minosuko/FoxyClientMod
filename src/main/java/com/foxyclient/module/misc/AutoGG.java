package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

/**
 * AutoGG - Sends "gg" after a game ends on minigame servers.
 * Detects game-end chat messages from common server networks
 * (Hypixel, Mineplex, etc.) and auto-sends the configured message.
 */
public class AutoGG extends Module {
    private final ModeSetting message = addSetting(new ModeSetting("Message", "What to say", "gg",
        "gg", "GG", "Good game!", "gg wp", "GG WP"));
    private final BoolSetting delay = addSetting(new BoolSetting("Delay", "Add short delay", true));
    private final NumberSetting delayMs = addSetting(new NumberSetting("DelayMs", "Delay in ms", 500, 100, 3000));

    private boolean sentGg = false;
    private long gameEndTime = 0;

    // Common game-end trigger phrases from popular servers
    private static final String[] TRIGGERS = {
        "winner", "won the game", "game over", "victory!", "you won!",
        "you lost!", "game ended", "has won", "wins!", "won the",
        "1st place", "first place", "top winner", "bedwars win",
        "the winning team", "winners:", "game end"
    };

    public AutoGG() {
        super("AutoGG", "Auto say GG after games", Category.MISC);
    }

    @Override
    public void onEnable() {
        sentGg = false;
        gameEndTime = 0;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString().toLowerCase();

            // Check if this looks like a game-end message
            for (String trigger : TRIGGERS) {
                if (text.contains(trigger)) {
                    if (!sentGg) {
                        gameEndTime = System.currentTimeMillis();
                        sentGg = true;

                        if (delay.get()) {
                            // Schedule sending after delay
                            long delayTime = delayMs.get().longValue();
                            new Thread(() -> {
                                try {
                                    Thread.sleep(delayTime);
                                    if (mc.player != null) {
                                        mc.player.networkHandler.sendChatMessage(message.get());
                                    }
                                } catch (InterruptedException ignored) {}
                            }).start();
                        } else {
                            mc.player.networkHandler.sendChatMessage(message.get());
                        }
                    }
                    break;
                }
            }

            // Reset after 10 seconds to allow for the next game
            if (sentGg && System.currentTimeMillis() - gameEndTime > 10000) {
                sentGg = false;
            }
        }
    }

    public String getMessage() { return message.get(); }
}
