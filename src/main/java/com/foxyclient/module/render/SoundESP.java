package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.PacketEvent;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.FoxyRenderer;
import com.foxyclient.util.RenderUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SoundESP - Visualizes sounds as expanding rings at their source location.
 * Shows sound category, name, and distance. Rings expand and fade over time.
 * Refactored for 1.21.11 to use FoxyRenderer.
 */
public class SoundESP extends Module {
    private final NumberSetting duration = addSetting(new NumberSetting("Duration", "Display duration (ms)", 3000, 500, 10000));
    private final NumberSetting maxSounds = addSetting(new NumberSetting("MaxSounds", "Max displayed sounds", 30, 5, 100));
    private final BoolSetting showDistance = addSetting(new BoolSetting("ShowDistance", "Show distance to sound", true));
    private final BoolSetting showTracers = addSetting(new BoolSetting("Tracers", "Draw tracers to sound sources", true));
    private final BoolSetting filterAmbient = addSetting(new BoolSetting("FilterAmbient", "Hide ambient sounds", true));
    private final BoolSetting filterWeather = addSetting(new BoolSetting("FilterWeather", "Hide weather sounds", true));
    private final BoolSetting filterBlocks = addSetting(new BoolSetting("FilterBlocks", "Hide block sounds", false));

    private final List<SoundEntry> sounds = new CopyOnWriteArrayList<>();

    // Color map for sound categories
    private static final Map<String, Color> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("entity", new Color(255, 100, 100));    // Red for entities
        CATEGORY_COLORS.put("hostile", new Color(255, 50, 50));     // Bright red for hostile
        CATEGORY_COLORS.put("player", new Color(100, 255, 100));    // Green for players
        CATEGORY_COLORS.put("block", new Color(100, 100, 255));     // Blue for blocks
        CATEGORY_COLORS.put("ambient", new Color(150, 150, 150));   // Gray for ambient
        CATEGORY_COLORS.put("weather", new Color(100, 200, 255));   // Cyan for weather
        CATEGORY_COLORS.put("music", new Color(255, 150, 255));     // Pink for music
        CATEGORY_COLORS.put("record", new Color(255, 200, 50));     // Gold for records
    }

    public SoundESP() {
        super("SoundESP", "Visualize sounds with expanding rings", Category.RENDER);
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (nullCheck()) return;
        if (!(event.getPacket() instanceof PlaySoundS2CPacket pkt)) return;

        String soundId = pkt.getSound().value().id().toString();

        // Filtering
        if (filterAmbient.get() && soundId.contains("ambient")) return;
        if (filterWeather.get() && (soundId.contains("rain") || soundId.contains("thunder"))) return;
        if (filterBlocks.get() && (soundId.contains("block.") || soundId.contains("place") || soundId.contains("break"))) return;

        Vec3d pos = new Vec3d(pkt.getX(), pkt.getY(), pkt.getZ());
        String category = extractCategory(soundId);
        String displayName = formatSoundName(soundId);
        Color color = CATEGORY_COLORS.getOrDefault(category, new Color(255, 255, 255));

        // Enforce max
        while (sounds.size() >= maxSounds.get().intValue()) {
            sounds.remove(0);
        }

        sounds.add(new SoundEntry(pos, displayName, category, color, System.currentTimeMillis()));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;
        long now = System.currentTimeMillis();
        long dur = duration.get().longValue();
        sounds.removeIf(s -> now - s.time > dur);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        long now = System.currentTimeMillis();
        long dur = duration.get().longValue();
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

        for (SoundEntry sound : sounds) {
            float progress = (float)(now - sound.time) / dur; // 0 to 1
            float alpha = 1.0f - progress;
            if (alpha <= 0) continue;

            // ESP marker at sound position
            BlockPos blockPos = BlockPos.ofFloored(sound.pos);
            Color fadeColor = new Color(
                sound.color.getRed(), sound.color.getGreen(), sound.color.getBlue(),
                (int)(alpha * 180));
            RenderUtil.drawBlockBox(event.getMatrices(), blockPos, fadeColor, 1.5f, event.getVertexConsumers());

            // Expanding ring effect
            drawExpandingRing(event.getMatrices(), sound.pos, camPos, sound.color, progress, alpha, event.getVertexConsumers());

            // Tracers
            if (showTracers.get()) {
                Color tracerColor = new Color(
                    sound.color.getRed(), sound.color.getGreen(), sound.color.getBlue(),
                    (int)(alpha * 150));
                RenderUtil.drawBlockTracer(event.getMatrices(), blockPos, tracerColor, event.getVertexConsumers());
            }
        }
    }

    private void drawExpandingRing(MatrixStack matrices, Vec3d center, Vec3d camPos, Color color,
                                    float progress, float alpha, VertexConsumerProvider vcp) {
        if (vcp == null) return;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = vcp.getBuffer(com.foxyclient.util.RenderLayers.getBypassLines());
        
        // Ring expands from 0.5 to 3.0 blocks radius
        float radius = 0.5f + progress * 2.5f;
        int segments = 24;

        double cx = center.x - camPos.x;
        double cy = center.y - camPos.y;
        double cz = center.z - camPos.z;

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = alpha * 0.8f;

        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;

            float x1 = (float)(cx + Math.cos(angle1) * radius);
            float z1 = (float)(cz + Math.sin(angle1) * radius);
            float x2 = (float)(cx + Math.cos(angle2) * radius);
            float z2 = (float)(cz + Math.sin(angle2) * radius);

            buffer.vertex(matrix, x1, (float) cy, z1).color(r, g, b, a);
            buffer.vertex(matrix, x2, (float) cy, z2).color(r, g, b, a);
        }
    }

    private String extractCategory(String soundId) {
        // e.g. "minecraft:entity.zombie.step" -> "entity"
        String path = soundId.contains(":") ? soundId.split(":")[1] : soundId;
        String[] parts = path.split("\\.");
        if (parts.length > 0) {
            String cat = parts[0].toLowerCase();
            if (cat.equals("entity")) {
                // Check if hostile
                if (path.contains("zombie") || path.contains("skeleton") || path.contains("creeper") ||
                    path.contains("spider") || path.contains("enderman") || path.contains("blaze") ||
                    path.contains("wither") || path.contains("phantom") || path.contains("pillager")) {
                    return "hostile";
                }
                if (path.contains("player")) return "player";
            }
            return cat;
        }
        return "unknown";
    }

    private String formatSoundName(String soundId) {
        // "minecraft:entity.zombie.step" -> "Zombie Step"
        String path = soundId.contains(":") ? soundId.split(":")[1] : soundId;
        String[] parts = path.split("\\.");
        if (parts.length >= 2) {
            String entity = capitalize(parts[parts.length - 2]);
            String action = capitalize(parts[parts.length - 1]);
            return entity + " " + action;
        }
        return path;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    public void onDisable() {
        sounds.clear();
    }

    private record SoundEntry(Vec3d pos, String name, String category, Color color, long time) {}
}
