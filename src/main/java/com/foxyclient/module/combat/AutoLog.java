package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.text.Text;

/**
 * Automatically disconnects when health is low or in danger.
 */
public class AutoLog extends Module {
    private final NumberSetting health = addSetting(new NumberSetting("Health", "Health to log at", 10.0, 1.0, 20.0));

    public AutoLog() {
        super("AutoLog", "Disconnects on low HP", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        if (mc.player.getHealth() <= health.get()) {
            mc.player.networkHandler.getConnection().disconnect(Text.literal("[AutoLog] Health reached " + health.get()));
            setEnabled(false);
        }
    }
}
