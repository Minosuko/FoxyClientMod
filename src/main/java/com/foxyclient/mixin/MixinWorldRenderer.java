package com.foxyclient.mixin;

import com.foxyclient.FoxyClient;
import com.foxyclient.event.events.RenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.foxyclient.module.render.MotionBlur;
import com.foxyclient.mixin.GameRendererAccessor;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.util.ObjectAllocator;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {
    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private net.minecraft.client.render.BufferBuilderStorage bufferBuilders;

    @Unique private Matrix4f prevModelView = new Matrix4f();
    @Unique private Matrix4f prevProjection = new Matrix4f();
    @Unique private Vector3f prevCameraPos = new Vector3f();

    @Inject(method = "render", at = @At("HEAD"))
    private void setMatrices(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f viewMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        GameRenderer gameRenderer = client.gameRenderer;
        float tickDelta = tickCounter.getTickProgress(true);
        float fov = ((GameRendererAccessor) gameRenderer).invokeGetFov(camera, tickDelta, true);
        
        MotionBlur module = MotionBlur.INSTANCE;
        if (module != null && module.getShader() != null) {
            module.getShader().setFrameMotionBlur(
                positionMatrix, 
                this.prevModelView, 
                gameRenderer.getBasicProjectionMatrix(fov), 
                this.prevProjection, 
                new Vector3f((float) (camera.getCameraPos().x % 30000.0D), (float) (camera.getCameraPos().y % 30000.0D), (float) (camera.getCameraPos().z % 30000.0D)), 
                this.prevCameraPos
            );
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void setOldMatrices(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f viewMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci) {
        GameRenderer gameRenderer = client.gameRenderer;
        this.prevModelView = new Matrix4f(positionMatrix);
        float tickDelta = tickCounter.getTickProgress(true);
        float fov = ((GameRendererAccessor) gameRenderer).invokeGetFov(camera, tickDelta, true);
        this.prevProjection = new Matrix4f(gameRenderer.getBasicProjectionMatrix(fov));
        this.prevCameraPos = new Vector3f((float) (camera.getCameraPos().x % 30000.0D), (float) (camera.getCameraPos().y % 30000.0D), (float) (camera.getCameraPos().z % 30000.0D));
    }

    // ─── Event dispatch (entity renders) ──────────────────────────────
    @Inject(method = "pushEntityRenders", at = @At("TAIL"))
    private void onPushEntityRenders(MatrixStack matrices, WorldRenderState renderStates, OrderedRenderCommandQueue queue, CallbackInfo ci) {
        if (FoxyClient.INSTANCE != null) {
            float tickDelta = client.getRenderTickCounter().getTickProgress(true);
            FoxyClient.INSTANCE.getEventBus().post(new RenderEvent(matrices, bufferBuilders.getEntityVertexConsumers(), tickDelta));
        }
    }
}
