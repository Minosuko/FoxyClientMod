package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.sound.SoundEvents;

import java.util.HashSet;
import java.util.Set;

/**
 * PlayerAlarms - Alerts when specific players join or leave the server.
 * Tracks the player list via PlayerListS2CPacket and notifies via chat
 * and optional sound alert when monitored players are detected.
 */
public class PlayerAlarms extends Module {
    private final BoolSetting soundAlert = addSetting(new BoolSetting("Sound", "Play sound on alert", true));
    private final BoolSetting allPlayers = addSetting(new BoolSetting("AllPlayers", "Alert on any join/leave", false));
    private final BoolSetting joinAlert = addSetting(new BoolSetting("Joins", "Alert on player joins", true));
    private final BoolSetting leaveAlert = addSetting(new BoolSetting("Leaves", "Alert on player leaves", true));

    private final Set<String> watchedPlayers = new HashSet<>();
    private final Set<String> onlinePlayers = new HashSet<>();
    private boolean initialized = false;

    public PlayerAlarms() {
        super("PlayerAlarms", "Alert on player join/leave", Category.MISC);
    }

    @Override
    public void onEnable() {
        onlinePlayers.clear();
        initialized = false;

        // Initialize with current player list
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().getPlayerList().forEach(entry -> {
                onlinePlayers.add(entry.getProfile().name());
            });
            initialized = true;
        }
    }

    @Override
    public void onDisable() {
        onlinePlayers.clear();
        initialized = false;
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (nullCheck()) return;

        if (event.getPacket() instanceof PlayerListS2CPacket packet) {
            // Handle player additions
            for (PlayerListS2CPacket.Entry entry : packet.getPlayerAdditionEntries()) {
                if (entry.profile() == null) continue;
                String name = entry.profile().name();
                if (name == null || name.isEmpty()) continue;

                if (!onlinePlayers.contains(name)) {
                    onlinePlayers.add(name);

                    if (initialized && joinAlert.get()) {
                        if (allPlayers.get() || watchedPlayers.contains(name.toLowerCase())) {
                            alertJoin(name);
                        }
                    }
                }
            }

            // Check for unlisted players (removals)
            for (var action : packet.getActions()) {
                if (action == PlayerListS2CPacket.Action.UPDATE_LISTED) {
                    for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
                        if (entry.profile() != null && !entry.listed()) {
                            String name = entry.profile().name();
                            if (name != null && onlinePlayers.remove(name)) {
                                if (leaveAlert.get()) {
                                    if (allPlayers.get() || watchedPlayers.contains(name.toLowerCase())) {
                                        alertLeave(name);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!initialized) initialized = true;
        }
    }

    private void alertJoin(String name) {
        info("§a+ §f" + name + " §7joined the server");
        playAlertSound();
    }

    private void alertLeave(String name) {
        info("§c- §f" + name + " §7left the server");
        playAlertSound();
    }

    private void playAlertSound() {
        if (soundAlert.get()) {
            mc.getSoundManager().play(PositionedSoundInstance.ui(
                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f
            ));
        }
    }

    public void addWatchedPlayer(String name) {
        watchedPlayers.add(name.toLowerCase());
        info("§aAdded §f" + name + " §ato watch list");
    }

    public void removeWatchedPlayer(String name) {
        watchedPlayers.remove(name.toLowerCase());
        info("§cRemoved §f" + name + " §cfrom watch list");
    }

    public Set<String> getWatchedPlayers() { return watchedPlayers; }
}
