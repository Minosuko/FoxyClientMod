package com.foxyclient.module.render;

import com.foxyclient.event.EventHandler;
import com.foxyclient.event.events.RenderEvent;
import com.foxyclient.module.Category;
import com.foxyclient.module.Module;
import com.foxyclient.setting.ColorSetting;
import com.foxyclient.setting.NumberSetting;
import com.foxyclient.util.RenderUtil;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class LightOverlay extends Module {

    private final NumberSetting range = addSetting(new NumberSetting("Range", "Block range to scan", 16.0, 5.0, 32.0));
    private final ColorSetting color = addSetting(new ColorSetting("Always Color", "Color for places where mobs always spawn", new Color(255, 0, 0, 255)));
    private final ColorSetting potentialColor = addSetting(new ColorSetting("Potential Color", "Color for places where mobs can spawn at night", new Color(255, 255, 0, 255)));

    public LightOverlay() {
        super("LightOverlay", "Shows blocks where mobs can spawn via X marks", Category.RENDER);
    }

    @EventHandler
    public void onRender(RenderEvent event) {
        if (nullCheck()) return;

        int r = range.get().intValue();
        BlockPos playerPos = mc.player.getBlockPos();

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Matrix4f matrix = new Matrix4f();
        // Use native linesTranslucent which has depth testing
        VertexConsumer buffer = event.getVertexConsumers().getBuffer(net.minecraft.client.render.RenderLayers.linesTranslucent());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    MobSpawn spawn = isValidMobSpawn(pos);

                    if (spawn != MobSpawn.Never) {
                        Color c = (spawn == MobSpawn.Potential) ? potentialColor.get() : color.get();
                        
                        float red = c.getRed() / 255f;
                        float green = c.getGreen() / 255f;
                        float blue = c.getBlue() / 255f;
                        float alpha = c.getAlpha() / 255f;

                        float tx = (float)(pos.getX() - camPos.x);
                        float ty = (float)(pos.getY() + 0.015 - camPos.y);
                        float tz = (float)(pos.getZ() - camPos.z);

                        // Draw X
                        RenderUtil.drawLine(buffer, matrix, tx, ty, tz, tx + 1f, ty, tz + 1f, red, green, blue, alpha, 2.0f);
                        RenderUtil.drawLine(buffer, matrix, tx + 1f, ty, tz, tx, ty, tz + 1f, red, green, blue, alpha, 2.0f);
                    }
                }
            }
        }
    }

    private MobSpawn isValidMobSpawn(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        boolean snow = state.getBlock() instanceof SnowBlock && state.get(SnowBlock.LAYERS) == 1;
        if (!state.isAir() && !snow) return MobSpawn.Never;

        if (!isValidSpawnBlock(mc.world.getBlockState(pos.down()))) return MobSpawn.Never;

        if (mc.world.getLightLevel(LightType.BLOCK, pos) > 0) return MobSpawn.Never;
        else if (mc.world.getLightLevel(LightType.SKY, pos) > 0) return MobSpawn.Potential;

        return MobSpawn.Always;
    }

    private boolean isValidSpawnBlock(BlockState state) {
        Block block = state.getBlock();

        if (block == Blocks.BEDROCK || block == Blocks.BARRIER || block instanceof TransparentBlock || block instanceof ScaffoldingBlock) {
            return false;
        }

        if (block == Blocks.SOUL_SAND || block == Blocks.MUD) return true;
        if (block instanceof SlabBlock && state.get(SlabBlock.TYPE) == SlabType.TOP) return true;
        if (block instanceof StairsBlock && state.get(StairsBlock.HALF) == BlockHalf.TOP) return true;

        return state.isOpaqueFullCube(); // A close approximation
    }

    public enum MobSpawn { Never, Potential, Always }
}
