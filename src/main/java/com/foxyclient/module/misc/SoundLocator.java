package com.foxyclient.module.misc;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.*;

/**
 * SoundLocator - Shows the source location of sounds with visual markers.
 * Intercepts PlaySoundS2CPacket to track where sounds originate and
 * renders colored boxes at those locations that fade over time.
 */
public class SoundLocator extends Module {
    private final NumberSetting duration = addSetting(new NumberSetting("Duration", "Marker duration (sec)", 5, 1, 30));
    private final BoolSetting chatLog = addSetting(new BoolSetting("ChatLog", "Log sounds in chat", false));
    private final BoolSetting filterHostile = addSetting(new BoolSetting("Hostile", "Show hostile mob sounds", true));
    private final BoolSetting filterPlayers = addSetting(new BoolSetting("Players", "Show player sounds", true));
    private final BoolSetting filterBlocks = addSetting(new BoolSetting("Blocks", "Show block sounds", true));
    private final BoolSetting filterOther = addSetting(new BoolSetting("Other", "Show other sounds", true));

    private final List<SoundMarker> markers = new ArrayList<>();

    public SoundLocator() {
        super("SoundLocator", "Show sound source locations", Category.MISC);
    }

    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (nullCheck()) return;
        if (event.getPacket() instanceof PlaySoundS2CPacket pkt) {
            String soundId = pkt.getSound().value().id().toString();

            // Filter by sound category
            if (!shouldShow(soundId)) return;

            BlockPos pos = new BlockPos((int) pkt.getX(), (int) pkt.getY(), (int) pkt.getZ());
            markers.add(new SoundMarker(pos, System.currentTimeMillis(), soundId));
            if (markers.size() > 100) markers.remove(0);

            if (chatLog.get()) {
                String shortName = soundId.contains(":") ? soundId.substring(soundId.indexOf(":") + 1) : soundId;
                double dist = Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pos));
                info("§e🔊 §f" + shortName + " §7at §f" + pos.toShortString()
                    + " §7(§f" + String.format("%.0f", dist) + "m§7)");
            }
        }
    }

    private boolean shouldShow(String soundId) {
        if (soundId.contains("hostile") || soundId.contains("zombie") ||
            soundId.contains("skeleton") || soundId.contains("creeper") ||
            soundId.contains("spider") || soundId.contains("phantom")) {
            return filterHostile.get();
        }
        if (soundId.contains("player") || soundId.contains("step") ||
            soundId.contains("eat") || soundId.contains("drink")) {
            return filterPlayers.get();
        }
        if (soundId.contains("block") || soundId.contains("chest") ||
            soundId.contains("door") || soundId.contains("piston")) {
            return filterBlocks.get();
        }
        return filterOther.get();
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;
        long now = System.currentTimeMillis();
        long durationMs = duration.get().longValue() * 1000;

        markers.removeIf(m -> now - m.time > durationMs);

        for (SoundMarker m : markers) {
            float alpha = 1.0f - (float)(now - m.time) / durationMs;
            Color color = getSoundColor(m.soundId, alpha);
            RenderUtil.drawBlockBox(event.getMatrices(), m.pos, color, 1.0f, event.getVertexConsumers());
        }
    }

    private Color getSoundColor(String soundId, float alpha) {
        int a = Math.max(0, Math.min(255, (int)(alpha * 200)));
        if (soundId.contains("hostile") || soundId.contains("zombie") ||
            soundId.contains("skeleton") || soundId.contains("creeper")) {
            return new Color(255, 50, 50, a); // Red for hostile
        }
        if (soundId.contains("player") || soundId.contains("step")) {
            return new Color(50, 255, 50, a); // Green for players
        }
        if (soundId.contains("block") || soundId.contains("chest")) {
            return new Color(50, 150, 255, a); // Blue for blocks
        }
        return new Color(255, 255, 50, a); // Yellow for other
    }

    @Override
    public void onDisable() {
        markers.clear();
    }

    private record SoundMarker(BlockPos pos, long time, String soundId) {}
}
