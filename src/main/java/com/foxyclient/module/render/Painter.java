package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.event.events.TickEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.BoolSetting;
import com.foxyclient.setting.ModeSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.*;

/**
 * Painter - Mark/paint blocks in the world with colored highlights.
 * Left-click to paint, right-click to erase. Supports multiple color modes.
 */
public class Painter extends Module {
    private final ModeSetting colorMode = addSetting(new ModeSetting("ColorMode", "How colors are chosen",
        "Rainbow", "Rainbow", "Solid", "Random", "Depth"));
    private final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Paint opacity", 0.5, 0.1, 1.0));
    private final NumberSetting red = addSetting(new NumberSetting("Red", "Solid color red", 255, 0, 255));
    private final NumberSetting green = addSetting(new NumberSetting("Green", "Solid color green", 50, 0, 255));
    private final NumberSetting blue = addSetting(new NumberSetting("Blue", "Solid color blue", 50, 0, 255));
    private final BoolSetting tracers = addSetting(new BoolSetting("Tracers", "Draw tracers to painted blocks", false));
    private final BoolSetting autoPaint = addSetting(new BoolSetting("AutoPaint", "Paint blocks you look at automatically", false));
    private final NumberSetting maxBlocks = addSetting(new NumberSetting("MaxBlocks", "Max painted blocks", 500, 50, 5000));

    private final Map<BlockPos, Color> paintedBlocks = new LinkedHashMap<>();
    private float rainbowHue = 0;
    private final Random rng = new Random();

    public Painter() {
        super("Painter", "Paint blocks with colored highlights", Category.RENDER);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (nullCheck()) return;

        rainbowHue += 0.005f;
        if (rainbowHue > 1f) rainbowHue = 0f;

        // Auto-paint mode: paint whatever block the crosshair is on
        if (autoPaint.get()) {
            if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = bhr.getBlockPos();
                if (!mc.world.getBlockState(pos).isAir() && !paintedBlocks.containsKey(pos)) {
                    paintBlock(pos);
                }
            }
        }

        // Manual paint: attack key = paint, use key = erase
        if (mc.options.attackKey.isPressed()) {
            if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                paintBlock(bhr.getBlockPos());
            }
        }
        if (mc.options.useKey.isPressed()) {
            if (mc.crosshairTarget instanceof BlockHitResult bhr && mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                paintedBlocks.remove(bhr.getBlockPos());
            }
        }
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (var entry : paintedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            Color color = entry.getValue();

            // Skip blocks too far away
            if (mc.player.getBlockPos().getManhattanDistance(pos) > 128) continue;

            // Render highlight
            Color renderColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int)(opacity.get() * 255));
            RenderUtil.drawBlockBox(event.getMatrices(), pos, renderColor, 1.5f, event.getVertexConsumers());

            // Tracers
            if (tracers.get()) {
                RenderUtil.drawBlockTracer(event.getMatrices(), pos, color, event.getVertexConsumers());
            }
        }
    }

    private void paintBlock(BlockPos pos) {
        if (mc.world.getBlockState(pos).isAir()) return;

        // Enforce max blocks
        while (paintedBlocks.size() >= maxBlocks.get().intValue()) {
            Iterator<BlockPos> it = paintedBlocks.keySet().iterator();
            if (it.hasNext()) { it.next(); it.remove(); }
        }

        Color color = getColor(pos);
        paintedBlocks.put(pos, color);
    }

    private Color getColor(BlockPos pos) {
        return switch (colorMode.get()) {
            case "Solid" -> new Color(red.get().intValue(), green.get().intValue(), blue.get().intValue());
            case "Random" -> new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            case "Depth" -> {
                // Color based on Y level: deep = blue, surface = green, sky = cyan
                float t = Math.clamp((pos.getY() + 64f) / 384f, 0f, 1f);
                yield new Color(
                    (int)(30 + t * 50),
                    (int)(50 + t * 200),
                    (int)(200 - t * 100)
                );
            }
            default -> { // Rainbow
                float hue = rainbowHue + (pos.getX() + pos.getZ()) * 0.01f;
                yield Color.getHSBColor(hue % 1f, 0.8f, 1.0f);
            }
        };
    }

    @Override
    public void onDisable() {
        paintedBlocks.clear();
    }

    public Map<BlockPos, Color> getPaintedBlocks() { return paintedBlocks; }
}
