package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.awt.*;

/**
 * Highlights players who are burrowed (inside a block).
 */
public class BurrowESP extends Module {
    private final ColorSetting color = addSetting(new ColorSetting("Color", "Burrow color", new Color(255, 128, 0, 150)));

    public BurrowESP() {
        super("BurrowESP", "Shows burrowed players", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            BlockPos pos = player.getBlockPos();
            if (!mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos).getBlock().getHardness() > 0) {
                RenderUtil.drawBlockBox(event.getMatrices(), pos, color.get(), 1.5f, event.getVertexConsumers());
            }
        }
    }
}
