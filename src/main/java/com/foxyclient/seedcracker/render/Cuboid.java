package com.foxyclient.seedcracker.render;

import com.foxyclient.util.FoxyRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class Cuboid {

    private final BlockPos pos;
    private final int argb;
    private final Box box;

    public Cuboid(BlockPos pos, int argb) {
        this(pos, new Vec3i(1, 1, 1), argb);
    }

    public Cuboid(BlockPos pos, Vec3i size, int argb) {
        this.pos = pos;
        this.argb = argb;
        this.box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + size.getX(), pos.getY() + size.getY(), pos.getZ() + size.getZ());
    }

    public Cuboid(BlockBox box, int argb) {
        this.pos = new BlockPos(box.getMinX(), box.getMinY(), box.getMinZ());
        this.argb = argb;
        this.box = new Box(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ());
    }

    public Cuboid(BlockPos pos, Map<BlockPos, BlockState> layout, int argb) {
        this.pos = pos;
        this.argb = argb;
        if (layout != null && !layout.isEmpty()) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos bp : layout.keySet()) {
                BlockPos absolute = pos.add(bp);
                minX = Math.min(minX, absolute.getX());
                minY = Math.min(minY, absolute.getY());
                minZ = Math.min(minZ, absolute.getZ());
                maxX = Math.max(maxX, absolute.getX() + 1);
                maxY = Math.max(maxY, absolute.getY() + 1);
                maxZ = Math.max(maxZ, absolute.getZ() + 1);
            }
            this.box = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            this.box = new Box(pos);
        }
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public Box getBox() {
        return this.box;
    }

    public int getColor() {
        return this.argb;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vcp, Vec3d cameraPos) {
        Box offsetBox = this.box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        int a = (this.argb >> 24) & 0xFF;
        int r = (this.argb >> 16) & 0xFF;
        int g = (this.argb >> 8) & 0xFF;
        int b = this.argb & 0xFF;
        
        if (a == 0) a = 255; 
        
        Color color = new Color(r, g, b, a);
        // Filled box (semi-transparent) + outline lines, matching FoxyClient ESP style
        FoxyRenderer.drawBox(matrices, vcp, offsetBox, color, 0.25f);
        FoxyRenderer.drawLines(matrices, vcp, offsetBox, color);
    }
}
