package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

/**
 * AutoAccept - Automatically accepts teleport and party requests.
 * Listens for incoming chat messages containing TPA or party invites
 * and auto-sends the appropriate accept command.
 */
public class AutoAccept extends Module {
    private final BoolSetting tpa = addSetting(new BoolSetting("TPA", "Accept teleport requests", true));
    private final BoolSetting party = addSetting(new BoolSetting("Party", "Accept party invites", true));
    private final BoolSetting tradeRequest = addSetting(new BoolSetting("Trade", "Accept trade requests", false));
    private final ModeSetting tpaCmd = addSetting(new ModeSetting("TPACmd", "TPA accept command",
        "/tpaccept", "/tpaccept", "/tpyes", "/accept"));

    public AutoAccept() {
        super("AutoAccept", "Auto accept requests", Category.MISC);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString().toLowerCase();
            String playerName = mc.player.getName().getString();

            // TPA detection
            if (tpa.get()) {
                if (text.contains("has requested to teleport") ||
                    text.contains("wants to teleport to you") ||
                    text.contains("has sent you a teleport request") ||
                    text.contains("/tpaccept") ||
                    text.contains("teleport request from")) {
                    mc.player.networkHandler.sendChatCommand(tpaCmd.get().substring(1)); // Remove leading /
                    info("§aAuto-accepted teleport request!");
                }
            }

            // Party detection
            if (party.get()) {
                if (text.contains("has invited you to their party") ||
                    text.contains("party invite from") ||
                    text.contains("invited you to join") ||
                    text.contains("/party accept")) {
                    mc.player.networkHandler.sendChatCommand("party accept");
                    info("§aAuto-accepted party invite!");
                }
            }

            // Trade detection
            if (tradeRequest.get()) {
                if (text.contains("wants to trade with you") ||
                    text.contains("trade request from") ||
                    text.contains("has sent you a trade request")) {
                    mc.player.networkHandler.sendChatCommand("trade accept");
                    info("§aAuto-accepted trade request!");
                }
            }
        }
    }
}
