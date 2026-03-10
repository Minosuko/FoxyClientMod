package com.foxyclient.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * Rendering utilities for ESP, Tracers, and other visual modules.
 * Refactored for 1.21.11 to use FoxyRenderer.
 */
public class RenderUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider vcp, Box box, Color color, float lineWidth) {
        // Fill
        FoxyRenderer.drawBox(matrices, vcp, box, color, 0.4f);
        
        // Lines
        FoxyRenderer.drawLines(matrices, vcp, box, color);
    }

    public static void drawEntityBox(MatrixStack matrices, Entity entity, Color color, float lineWidth, float tickDelta, VertexConsumerProvider vcp) {
        Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        
        // Offset the bounding box by the entity's position to get it relative to 0,0,0
        // Then add the lerped position, and subtract the camera position
        Box box = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ())
            .offset(lerpedPos.x - camPos.x, lerpedPos.y - camPos.y, lerpedPos.z - camPos.z);
            
        drawBox(matrices, vcp, box, color, lineWidth);
    }

    public static void drawBlockBox(MatrixStack matrices, BlockPos pos, Color color, float lineWidth, VertexConsumerProvider vcp) {
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        double x = pos.getX() - camPos.x;
        double y = pos.getY() - camPos.y;
        double z = pos.getZ() - camPos.z;

        Box box = new Box(x, y, z, x + 1, y + 1, z + 1);
        drawBox(matrices, vcp, box, color, lineWidth);
    }

    public static void drawTracerLine(MatrixStack matrices, Entity entity, Color color, float tickDelta, VertexConsumerProvider vcp) {
        if (mc.player == null) return;

        Vec3d entityPos = entity.getLerpedPos(tickDelta).add(0, entity.getHeight() / 2.0, 0);
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        
        // Pitch and Yaw to get forward vector for crosshair
        float pitch = mc.gameRenderer.getCamera().getPitch();
        float yaw = mc.gameRenderer.getCamera().getYaw();
        Vec3d forward = Vec3d.fromPolar(pitch, yaw).normalize().multiply(1.0); // 1 block forward from camera

        float startX = (float) forward.x;
        float startY = (float) forward.y;
        float startZ = (float) forward.z;

        float endX = (float) (entityPos.x - camPos.x);
        float endY = (float) (entityPos.y - camPos.y);
        float endZ = (float) (entityPos.z - camPos.z);

        FoxyRenderer.drawTracer(matrices, vcp, startX, startY, startZ, endX, endY, endZ, color);
    }

    /** Draw a tracer line from the camera to a block position. */
    public static void drawBlockTracer(MatrixStack matrices, BlockPos pos, Color color, VertexConsumerProvider vcp) {
        if (mc.player == null) return;

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d target = Vec3d.ofCenter(pos);
        
        float pitch = mc.gameRenderer.getCamera().getPitch();
        float yaw = mc.gameRenderer.getCamera().getYaw();
        Vec3d forward = Vec3d.fromPolar(pitch, yaw).normalize().multiply(1.0);

        float startX = (float) forward.x;
        float startY = (float) forward.y;
        float startZ = (float) forward.z;

        float endX = (float) (target.x - camPos.x);
        float endY = (float) (target.y - camPos.y);
        float endZ = (float) (target.z - camPos.z);

        FoxyRenderer.drawTracer(matrices, vcp, startX, startY, startZ, endX, endY, endZ, color);
    }

    public static void drawNametag(MatrixStack matrices, String text, double x, double y, double z, float scale) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        
        matrices.push();
        matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        matrices.multiply(mc.gameRenderer.getCamera().getRotation());
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = mc.textRenderer;
        float textWidth = textRenderer.getWidth(text) / 2.0f;
        
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        
        // Draw background
        int bgColor = mc.options.getTextBackgroundColor(0.25f);
        textRenderer.draw(
            text, 
            -textWidth, 
            0, 
            0xFFFFFFFF, 
            false, 
            matrix4f, 
            immediate, 
            TextRenderer.TextLayerType.NORMAL, 
            bgColor, 
            LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        matrices.pop();
    }
}
