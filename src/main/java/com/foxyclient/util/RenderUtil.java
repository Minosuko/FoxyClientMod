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
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d center = Vec3d.ofCenter(pos);
        matrices.push();
        FoxyRenderer.drawTracer(matrices, vcp, 0, 0, 0, (float)(center.x - camPos.x), (float)(center.y - camPos.y), (float)(center.z - camPos.z), color);
        matrices.pop();
    }

    public static void drawTracerLine(MatrixStack matrices, Entity entity, Color color, float tickDelta, VertexConsumerProvider.Immediate vcp) {
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        Vec3d pos = entity.getLerpedPos(tickDelta);
        matrices.push();
        FoxyRenderer.drawTracer(matrices, vcp, 0, 0, 0, (float)(pos.x - camPos.x), (float)(pos.y - camPos.y), (float)(pos.z - camPos.z), color);
        matrices.pop();
    }
}
