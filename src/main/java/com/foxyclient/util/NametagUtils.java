package com.foxyclient.util;

import com.foxyclient.module.ui.Nametags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class NametagUtils {
    private static final Identifier BADGE_TEXTURE = Identifier.of("foxyclient", "textures/badge.png");

    public static void renderBadge(EntityRenderState state, Text text,
                                    MatrixStack matrices, OrderedRenderCommandQueue queue,
                                    CameraRenderState cameraState) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCameraEntity() == null) return;

        int textWidth = client.textRenderer.getWidth(text);
        float badgeSize = 8.0F;
        float yOffset = state.height + 0.5F;

        matrices.push();
        matrices.translate(0.0, yOffset, 0.0);
        matrices.multiply(cameraState.orientation);
        matrices.scale(-0.025F, -0.025F, 0.025F);

        float badgeX = (textWidth / 2.0F) - badgeSize - 1;
        float badgeY = -badgeSize / 9.0F;

        final int light = state.light;
        
        Nametags module = Nametags.INSTANCE;
        boolean perspective = module != null && module.perspective.get();
        final int iconLight = perspective ? 0xF000F0 : light;

        matrices.push();
        matrices.translate(badgeX, badgeY, 0);

        queue.submitCustom(matrices, RenderLayers.entityTranslucent(BADGE_TEXTURE), (entry, buffer) -> {
            Matrix4f matrix = entry.getPositionMatrix();

            buffer.vertex(matrix, 0, badgeSize, 0)
                    .color(0xFFFFFFFF)
                    .texture(0, 1)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(iconLight)
                    .normal(0, 0, 1);
            buffer.vertex(matrix, badgeSize, badgeSize, 0)
                    .color(0xFFFFFFFF)
                    .texture(1, 1)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(iconLight)
                    .normal(0, 0, 1);
            buffer.vertex(matrix, badgeSize, 0, 0)
                    .color(0xFFFFFFFF)
                    .texture(1, 0)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(iconLight)
                    .normal(0, 0, 1);
            buffer.vertex(matrix, 0, 0, 0)
                    .color(0xFFFFFFFF)
                    .texture(0, 0)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(iconLight)
                    .normal(0, 0, 1);
        });

        matrices.pop();
        matrices.pop();
    }
}
