package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.util.RenderUtil;
import com.foxyclient.mixin.WorldRendererAccessor;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;
import java.util.Collection;

/**
 * Shows block breaking progress.
 */
public class BreakIndicators extends Module {
    public BreakIndicators() {
        super("BreakIndicators", "Shows breaking progress", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck() || mc.worldRenderer == null) return;

        // Access the map of blocks currently being broken via our Mixin Accessor
        Collection<BlockBreakingInfo> breakageInfos = ((WorldRendererAccessor) mc.worldRenderer).getBlockBreakingInfos().values();

        for (BlockBreakingInfo info : breakageInfos) {
            BlockPos pos = info.getPos();
            int stage = info.getStage(); // 0 to 9 representing the breaking stage
            
            // Calculate color based on stage (Green = 0, Yellow = 5, Red = 9)
            // The stage max is 9, if it reaches 10 the block is broken.
            float percent = stage / 9.0f;
            percent = Math.max(0.0f, Math.min(1.0f, percent));
            
            // Green -> Red Gradient using HSB
            // Green hue is ~0.33, Red hue is 0.0
            float hue = (1.0f - percent) * 0.33f;
            Color color = Color.getHSBColor(hue, 1.0f, 1.0f);

            // Draw a bounding box around the active block being broken
            RenderUtil.drawBlockBox(
                event.getMatrices(),
                pos,
                color,
                1.5f,
                event.getVertexConsumers()
            );
        }
    }
}
