package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;

/**
 * AutoLeave - Auto disconnect when certain conditions are met.
 * Can disconnect based on health threshold or when specific players
 * are spotted within visual range.
 */
public class AutoLeave extends Module {
    private final ModeSetting mode = addSetting(new ModeSetting("Mode", "Disconnect method",
        "Disconnect", "Disconnect", "Command"));
    private final NumberSetting health = addSetting(new NumberSetting("Health", "Health threshold", 6, 0, 20));
    private final BoolSetting onPlayer = addSetting(new BoolSetting("OnPlayer", "Leave when player spotted", false));
    private final NumberSetting playerRange = addSetting(new NumberSetting("Range", "Player detection range", 32, 5, 128));
    private final BoolSetting ignoreCreative = addSetting(new BoolSetting("IgnoreCreative", "Ignore creative players", true));

    public AutoLeave() {
        super("AutoLeave", "Auto disconnect on conditions", Category.MISC);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        // Health check
        float currentHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (currentHealth <= health.get()) {
            disconnect("§cHealth below threshold (" + String.format("%.1f", currentHealth) + " HP)");
            return;
        }

        // Player proximity check
        if (onPlayer.get()) {
            double range = playerRange.get();
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (ignoreCreative.get() && player.getAbilities().creativeMode) continue;

                if (mc.player.distanceTo(player) <= range) {
                    disconnect("§cPlayer spotted: " + player.getName().getString());
                    return;
                }
            }
        }
    }

    private void disconnect(String reason) {
        info(reason);
        switch (mode.get()) {
            case "Disconnect" -> {
                mc.player.networkHandler.getConnection().disconnect(
                    net.minecraft.text.Text.literal("[FoxyClient] " + reason)
                );
            }
            case "Command" -> {
                mc.player.networkHandler.sendChatCommand("quit");
            }
        }
        toggle();
    }
}
