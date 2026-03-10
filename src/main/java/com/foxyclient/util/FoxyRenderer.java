package com.foxyclient.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

public class FoxyRenderer {
    // We will now use VertexConsumerProvider for ESP to be 1.21.4 compatible
    public static void drawBox(MatrixStack matrices, VertexConsumerProvider vcp, Box box, Color color, float alpha) {
        if (vcp == null) return;
        
        // Use a custom RenderLayer that always bypasses depth test
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassTranslucent());

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = alpha;

        // Bottom
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);

        // Top
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        // ... North, South, West, East vertices here ...
        // Logic remains same but using 'buffer' vertex calls
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);

        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(r, g, b, a);
        buffer.vertex(matrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(r, g, b, a);
        
        // No draw() call here, VCP handles it!
    }

    public static void drawLines(MatrixStack matrices, VertexConsumerProvider vcp, Box box, Color color) {
        if (vcp == null) return;
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassLines());

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        // Replicate logic using 'buffer'
        drawInnerLines(buffer, matrix, box, r, g, b, a);
    }

    private static void drawInnerLines(VertexConsumer buffer, Matrix4f matrix, Box box, float r, float g, float b, float a) {
        // Bottom
        vertexLine(buffer, matrix, (float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.minY, (float)box.minZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.minY, (float)box.maxZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ, (float)box.minX, (float)box.minY, (float)box.maxZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.minX, (float)box.minY, (float)box.maxZ, (float)box.minX, (float)box.minY, (float)box.minZ, r, g, b, a);

        // Top
        vertexLine(buffer, matrix, (float)box.minX, (float)box.maxY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.minZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.maxY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.maxY, (float)box.maxZ, (float)box.minX, (float)box.maxY, (float)box.maxZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.minX, (float)box.maxY, (float)box.maxZ, (float)box.minX, (float)box.maxY, (float)box.minZ, r, g, b, a);

        // Verticals
        vertexLine(buffer, matrix, (float)box.minX, (float)box.minY, (float)box.minZ, (float)box.minX, (float)box.maxY, (float)box.minZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.minZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.maxX, (float)box.minY, (float)box.maxZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ, r, g, b, a);
        vertexLine(buffer, matrix, (float)box.minX, (float)box.minY, (float)box.maxZ, (float)box.minX, (float)box.maxY, (float)box.maxZ, r, g, b, a);
    }

    private static void vertexLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; } else { dx = 1; dy = 0; dz = 0; }
        
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).normal(dx, dy, dz).lineWidth(1.0f);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).normal(dx, dy, dz).lineWidth(1.0f);
    }

    public static void drawTracer(MatrixStack matrices, VertexConsumerProvider vcp, float startX, float startY, float startZ, float endX, float endY, float endZ, Color color) {
        if (vcp == null) return;
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassLines());

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float dx = endX - startX;
        float dy = endY - startY;
        float dz = endZ - startZ;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) { dx /= len; dy /= len; dz /= len; } else { dx = 1; dy = 0; dz = 0; }

        buffer.vertex(matrix, startX, startY, startZ).color(r, g, b, a).normal(dx, dy, dz).lineWidth(1.0f);
        buffer.vertex(matrix, endX, endY, endZ).color(r, g, b, a).normal(dx, dy, dz).lineWidth(1.0f);
    }
}
