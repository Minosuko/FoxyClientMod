package com.foxyclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.awt.Color;

public class RenderUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Identifier LOGO = Identifier.of("foxyclient", "icon.png");

    public static void drawNametag(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, String text, double x, double y, double z, float scale) {
        if (matrices == null) return;
        
        // System.out.println("[RenderUtil] drawNametag: " + text);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        
        matrices.push();
        // If x,y,z are 0, this assumes matrices are already at the entity
        matrices.translate(x == 0 && y == 0 && z == 0 ? 0 : x - camPos.x, y == 0 && x == 0 && z == 0 ? 0 : y - camPos.y, z == 0 && x == 0 && y == 0 ? 0 : z - camPos.z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = mc.textRenderer;
        float textWidth = textRenderer.getWidth(text);
        float halfWidth = textWidth / 2.0f;
        
        VertexConsumerProvider.Immediate immediate = vcp != null ? vcp : mc.getBufferBuilders().getEntityVertexConsumers();
        
        // Draw background
        int bgColor = mc.options.getTextBackgroundColor(0.25f);
        
        // Draw centered text
        textRenderer.draw(
            text, 
            -halfWidth, 
            0, 
            0xFFFFFFFF, 
            false, 
            matrix4f, 
            immediate, 
            TextRenderer.TextLayerType.NORMAL, 
            bgColor, 
            LightmapTextureManager.MAX_LIGHT_COORDINATE
        );

        // Draw FoxyClient logo to the right
        float logoSize = 10.0f;
        float logoX = halfWidth + 2.0f;
        float logoY = -1.0f;
        
        RenderLayer logoLayer = RenderLayer.of("foxyclient_logo", 
            RenderSetup.builder(net.minecraft.client.gl.RenderPipelines.ENTITY_CUTOUT)
                .texture("Sampler0", LOGO)
                .useLightmap()
                .useOverlay()
                .outlineMode(RenderSetup.OutlineMode.AFFECTS_OUTLINE)
                .build()
        );
        
        VertexConsumer vc = immediate.getBuffer(logoLayer);
        // Using correct vertex elements: position, color, texture, overlay, light, normal
        vc.vertex(matrix4f, logoX, logoY + logoSize, 0).color(1f, 1f, 1f, 1f).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        vc.vertex(matrix4f, logoX + logoSize, logoY + logoSize, 0).color(1f, 1f, 1f, 1f).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        vc.vertex(matrix4f, logoX + logoSize, logoY, 0).color(1f, 1f, 1f, 1f).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);
        vc.vertex(matrix4f, logoX, logoY, 0).color(1f, 1f, 1f, 1f).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0, 0, 1);

        matrices.pop();
    }

    public static void drawBlockBox(MatrixStack matrices, BlockPos pos, Color color, float lineWidth, VertexConsumerProvider.Immediate vcp) {
        Box box = new Box(pos);
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        FoxyRenderer.drawBox(matrices, vcp, box, color, 0.4f);
        FoxyRenderer.drawLines(matrices, vcp, box, color);
        matrices.pop();
    }

    public static void drawEntityBox(MatrixStack matrices, Entity entity, Color color, float lineWidth, float tickDelta, VertexConsumerProvider.Immediate vcp) {
        Box box = entity.getBoundingBox();
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        FoxyRenderer.drawBox(matrices, vcp, box, color, 0.4f);
        FoxyRenderer.drawLines(matrices, vcp, box, color);
        matrices.pop();
    }

    public static void drawBox(MatrixStack matrices, VertexConsumerProvider.Immediate vcp, Box box, Color color, float alpha) {
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        FoxyRenderer.drawBox(matrices, vcp, box, color, alpha);
        matrices.pop();
    }

    public static void drawBlockTracer(MatrixStack matrices, BlockPos pos, Color color, VertexConsumerProvider.Immediate vcp) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        // Compute un-bobbed crosshair position from camera yaw/pitch
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        Vec3d playerPos = mc.player.getLerpedPos(tickDelta);
        Vec3d eyePos = playerPos.add(0, mc.player.getStandingEyeHeight(), 0);
        float yaw = camera.getYaw();
        float pitch = camera.getPitch();
        double yawRad = Math.toRadians(-yaw);
        double pitchRad = Math.toRadians(-pitch);
        double lookX = Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3d crosshairPos = eyePos.add(lookX, lookY, lookZ);

        // Start from crosshair in camera-relative coords
        float sx = (float)(crosshairPos.x - camPos.x);
        float sy = (float)(crosshairPos.y - camPos.y);
        float sz = (float)(crosshairPos.z - camPos.z);

        Vec3d center = Vec3d.ofCenter(pos);

        // Use a clean identity matrix to avoid camera bob/shake waving
        Matrix4f matrix = new Matrix4f();
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassTranslucent());

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float tx = (float)(center.x - camPos.x);
        float ty = (float)(center.y - camPos.y);
        float tz = (float)(center.z - camPos.z);

        drawLineQuad(buffer, matrix, sx, sy, sz, tx, ty, tz, 0.01f, r, g, b, a);
    }

    public static void drawTracerLine(MatrixStack matrices, Entity entity, Color color, float tickDelta, VertexConsumerProvider.Immediate vcp) {
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d pos = entity.getLerpedPos(tickDelta);

        Matrix4f matrix = new Matrix4f();
        VertexConsumer buffer = vcp.getBuffer(RenderLayers.getBypassTranslucent());

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        float tx = (float)(pos.x - camPos.x);
        float ty = (float)(pos.y - camPos.y);
        float tz = (float)(pos.z - camPos.z);

        drawLineQuad(buffer, matrix, 0, 0, 0, tx, ty, tz, 0.01f, r, g, b, a);
    }

    /**
     * Draw a line as a thin camera-facing quad using POSITION_COLOR (QUADS).
     */
    private static void drawLineQuad(VertexConsumer buffer, Matrix4f matrix,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      float halfWidth,
                                      float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;

        org.joml.Vector3f lineDir = new org.joml.Vector3f(dx / len, dy / len, dz / len);

        // View direction from camera to midpoint
        float mx = (x1 + x2) * 0.5f, my = (y1 + y2) * 0.5f, mz = (z1 + z2) * 0.5f;
        float mLen = (float) Math.sqrt(mx * mx + my * my + mz * mz);
        org.joml.Vector3f viewDir = mLen < 0.001f
                ? new org.joml.Vector3f(x2, y2, z2).normalize()
                : new org.joml.Vector3f(mx / mLen, my / mLen, mz / mLen);

        // Perpendicular = cross(lineDir, viewDir)
        org.joml.Vector3f perp = new org.joml.Vector3f();
        lineDir.cross(viewDir, perp);
        float perpLen = perp.length();
        if (perpLen < 0.0001f) {
            org.joml.Vector3f up = Math.abs(lineDir.y) > 0.99f ? new org.joml.Vector3f(1, 0, 0) : new org.joml.Vector3f(0, 1, 0);
            lineDir.cross(up, perp);
            perpLen = perp.length();
            if (perpLen < 0.0001f) return;
        }
        perp.mul(halfWidth / perpLen);

        buffer.vertex(matrix, x1 - perp.x, y1 - perp.y, z1 - perp.z).color(r, g, b, a);
        buffer.vertex(matrix, x1 + perp.x, y1 + perp.y, z1 + perp.z).color(r, g, b, a);
        buffer.vertex(matrix, x2 + perp.x, y2 + perp.y, z2 + perp.z).color(r, g, b, a);
        buffer.vertex(matrix, x2 - perp.x, y2 - perp.y, z2 - perp.z).color(r, g, b, a);
    }
}