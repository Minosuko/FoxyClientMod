package com.foxyclient.module.misc;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * AutoReconnect - Automatically reconnects when disconnected from a server.
 * Monitors the disconnect screen and auto-reconnects after the configured delay.
 */
public class AutoReconnect extends Module {
    private final NumberSetting delay = addSetting(new NumberSetting("Delay", "Reconnect delay (seconds)", 5, 1, 60));
    private final BoolSetting notify = addSetting(new BoolSetting("Notify", "Show reconnect countdown", true));

    private ServerInfo lastServer = null;
    private boolean reconnecting = false;

    public AutoReconnect() {
        super("AutoReconnect", "Auto reconnect when kicked", Category.MISC);
    }

    @Override
    public void onTick() {
        // Store current server info while connected
        if (mc.getCurrentServerEntry() != null && mc.player != null) {
            lastServer = mc.getCurrentServerEntry();
        }

        // Check if we're on the disconnected screen and should reconnect
        if (mc.currentScreen instanceof DisconnectedScreen && lastServer != null && !reconnecting) {
            reconnecting = true;
            long delayMs = delay.get().longValue() * 1000;

            if (notify.get()) {
                System.out.println("[FoxyClient] Reconnecting in " + delay.get().intValue() + "s...");
            }

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    mc.execute(() -> {
                        if (lastServer != null) {
                            ServerAddress address = ServerAddress.parse(lastServer.address);
                            ConnectScreen.connect(
                                new MultiplayerScreen(new TitleScreen()),
                                mc, address, lastServer, false, null
                            );
                        }
                        reconnecting = false;
                    });
                } catch (InterruptedException ignored) {
                    reconnecting = false;
                }
            }).start();
        }
    }

    public double getDelay() { return delay.get(); }
}
