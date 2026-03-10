package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

/**
 * AutoLogin - Automatically sends /login or /register commands on server join.
 * Detects server auth prompts in chat and responds with the stored password.
 */
public class AutoLogin extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Auth mode", "Login", "Login", "Register"));
    private final NumberSetting delayTicks = addSetting(new NumberSetting("Delay", "Ticks to wait before sending", 20, 0, 100));

    private String password = "";
    private boolean sent = false;
    private boolean detected = false;
    private int waitTicks = 0;

    public AutoLogin() {
        super("AutoLogin", "Auto login/register on servers", Category.MISC);
    }

    @Override
    public void onEnable() {
        sent = false;
        detected = false;
        waitTicks = 0;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck() || sent) return;

        // Detect login/register prompts in chat
        if (event.getPacket() instanceof GameMessageS2CPacket packet) {
            String text = packet.content().getString().toLowerCase();
            if (text.contains("/login") || text.contains("/register") ||
                text.contains("/l ") || text.contains("/reg") ||
                text.contains("please authenticate") ||
                text.contains("please login") ||
                text.contains("please register")) {
                detected = true;
                waitTicks = 0;
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck() || sent || password.isEmpty()) return;

        if (detected) {
            waitTicks++;
            if (waitTicks >= delayTicks.get().intValue()) {
                switch (mode.get()) {
                    case "Login" -> mc.player.networkHandler.sendChatCommand("login " + password);
                    case "Register" -> mc.player.networkHandler.sendChatCommand("register " + password + " " + password);
                }
                sent = true;
                info("§aAuto-authenticated!");
            }
        }
    }

    public void setPassword(String password) { this.password = password; }
    public String getPassword() { return password; }
}
