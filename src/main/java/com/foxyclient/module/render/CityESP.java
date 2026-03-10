package com.foxyclient.module.render;

import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.VertexConsumerProvider;
import com.foxyclient.event.events.RenderEvent;

import java.awt.*;

/**
 * Shows cityable blocks around enemies (CityESP).
 */
public class CityESP extends Module {
    private final ColorSetting color = addSetting(new ColorSetting("Color", "City block color", new Color(255, 0, 0, 150)));

    public CityESP() {
        super("CityESP", "Shows cityable blocks", Category.RENDER);
    }

    @com.foxyclient.event.EventHandler
    public void onRender(com.foxyclient.event.events.RenderEvent event) {
        render(event.getMatrices(), event);
    }

    public void render(MatrixStack matrices, RenderEvent event) {
        if (nullCheck()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (mc.player.distanceTo(player) > 10) continue;

            BlockPos pos = player.getBlockPos();
            BlockPos[] cityPoints = { pos.north(), pos.south(), pos.east(), pos.west() };

            for (BlockPos p : cityPoints) {
                if (mc.world.getBlockState(p).getBlock().getHardness() > 0) {
                    RenderUtil.drawBlockBox(matrices, p, color.get(), 1.5f, event.getVertexConsumers());
                }
            }
        }
    }
}
