package com.foxyclient.module.combat;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects and ignores fake/bot players based on various criteria.
 */
public class AntiBot extends Module {
    private final BoolSetting gamemode = addSetting(new BoolSetting("Gamemode", "Filter by gamemode", true));
    private final BoolSetting ping = addSetting(new BoolSetting("Ping", "Filter by ping", true));
    private final BoolSetting tablist = addSetting(new BoolSetting("TabList", "Must be in tab list", true));
    private final NumberSetting maxPing = addSetting(new NumberSetting("MaxPing", "Max ping to be real", 1, 0, 10));

    private final Set<Integer> botIds = new HashSet<>();

    public AntiBot() {
        super("AntiBot", "Ignore fake/bot players", Category.COMBAT);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        botIds.clear();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;

            boolean isBot = false;

            // Check if in tab list
            if (tablist.get()) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry == null) isBot = true;
            }

            // Check ping (bots usually have 0 ping)
            if (ping.get() && !isBot) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
                if (entry != null && entry.getLatency() <= maxPing.get()) {
                    isBot = true;
                }
            }

            if (isBot) botIds.add(entity.getId());
        }
    }

    public boolean isBot(Entity entity) {
        return isEnabled() && botIds.contains(entity.getId());
    }
}
