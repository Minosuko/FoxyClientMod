package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import com.foxyclient.util.WorldUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.VertexConsumerProvider;
import com.foxyclient.event.events.RenderEvent;

import java.awt.*;

/**
 * Advanced hole detection and coloring (HoleESP+).
 */
public class HoleESP extends Module {
    private final NumberSetting range = addSetting(new NumberSetting("Range", "Scan range", 8.0, 1.0, 16.0));
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Hole color", new Color(0, 255, 0, 100)));

    public HoleESP() {
        super("HoleESP+", "Advanced hole detection", Category.RENDER);
    }

    @com.foxyclient.event.EventHandler
    public void onRender(com.foxyclient.event.events.RenderEvent event) {
        render(event.getMatrices(), event);
    }

    public void render(MatrixStack matrices, RenderEvent event) {
        if (nullCheck()) return;

        BlockPos playerPos = mc.player.getBlockPos();
        int r = range.get().intValue();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (WorldUtil.isHole(pos)) {
                        RenderUtil.drawBlockBox(matrices, pos, color.get(), 1.0f, event.getVertexConsumers());
                    }
                }
            }
        }
    }
}
