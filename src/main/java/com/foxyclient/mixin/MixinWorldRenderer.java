package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.RenderEvent;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.util.ObjectAllocator;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import org.joml.Vector4f;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, net.minecraft.client.render.Camera camera, Matrix4f positionMatrix, Matrix4f basicProjectionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null) {
            MatrixStack matrices = new MatrixStack();
            matrices.multiplyPositionMatrix(positionMatrix);
            FoxyClient.INSTANCE.getEventBus().post(new RenderEvent(matrices, null, tickCounter.getTickProgress(false)));
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, net.minecraft.client.render.Camera camera, Matrix4f positionMatrix, Matrix4f basicProjectionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (FoxyClient.INSTANCE == null) return;
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);
        net.minecraft.client.render.VertexConsumerProvider.Immediate vcp = net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        FoxyClient.INSTANCE.getEventBus().post(new RenderEvent(matrices, vcp, tickCounter.getTickProgress(false)));
        vcp.draw(com.foxyclient.util.RenderLayers.getBypassTranslucent());
        vcp.draw(net.minecraft.client.render.RenderLayers.LINES);
    }
}
