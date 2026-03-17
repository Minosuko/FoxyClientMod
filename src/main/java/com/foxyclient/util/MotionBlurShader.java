package com.foxyclient.util;

import com.foxyclient.mixin.PostEffectPassAccessor;
import com.foxyclient.mixin.PostEffectProcessorAccessor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MotionBlurShader {
    private static final Logger LOGGER = LoggerFactory.getLogger("FoxyClient/MotionBlur");
    private static final Identifier SHADER_ID = Identifier.of("foxyclient", "motion_blur");

    private PostEffectProcessor processor;
    private boolean initialized = false;
    private boolean errored = false;

    private final Matrix4f mvInverse = new Matrix4f();
    private final Matrix4f projInverse = new Matrix4f();
    private final Matrix4f prevModelView = new Matrix4f();
    private final Matrix4f prevProjection = new Matrix4f();

    private float cameraPosX;
    private float cameraPosY;
    private float cameraPosZ;

    private float prevCameraPosX;
    private float prevCameraPosY;
    private float prevCameraPosZ;

    private float viewResX = 1.0F;
    private float viewResY = 1.0F;
    
    private float blendFactor = 0.5F;
    private float inverseSamples = 1.0F;
    private float handDepthThreshold = 0.56F;
    
    private int motionBlurSamples = 0;
    private int halfSamples = 0;
    private int blurAlgorithm = 1;

    public void ensureInitialized() {
        if (!initialized && !errored) {
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                ShaderLoader shaderLoader = client.getShaderLoader();
                // Accessing cache via reflection/Mixin
                // the loadPostEffect method is public in ShaderLoader, but let's just use it directly!
                // Wait, ShaderLoader.loadPostEffect is public. No need for the cache hack down to loadProcessor!
                // Signature: loadPostEffect(Identifier id, Set<Identifier> availableExternalTargets);
                this.processor = shaderLoader.loadPostEffect(SHADER_ID, DefaultFramebufferSet.MAIN_ONLY);
                if (this.processor != null) {
                    this.initialized = true;
                    LOGGER.info("Shader loaded: {}", SHADER_ID);
                } else {
                    this.errored = true;
                    LOGGER.error("Failed to load shader {}; returned null", SHADER_ID);
                }
            } catch (Exception e) {
                this.errored = true;
                LOGGER.error("Exception loading shader {}", SHADER_ID, e);
            }
        }
    }

    public void render(float tickDelta) {
        ensureInitialized();
        if (this.processor != null) {
            replaceUniformBuffer();
            MinecraftClient client = MinecraftClient.getInstance();
            this.processor.render(client.getFramebuffer(), ObjectAllocator.TRIVIAL);
            // Note: GameRenderer blur uses: postEffectProcessor.render(this.client.getFramebuffer(), this.pool); 
            // In 1.21.11, the simple processor.render requires Framebuffer and ObjectAllocator.
            // Wait, we need the ObjectAllocator... Let's check signature.
        }
    }

    private void replaceUniformBuffer() {
        if (this.processor != null) {
            List<PostEffectPass> passes = ((PostEffectProcessorAccessor) this.processor).getPasses();
            if (passes != null && !passes.isEmpty()) {
                PostEffectPass motionBlurPass = passes.get(0);
                Map<String, GpuBuffer> uniformBuffers = ((PostEffectPassAccessor) motionBlurPass).getUniformBuffers();
                
                if (uniformBuffers.containsKey("MotionBlurParams")) {
                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        Std140Builder builder = Std140Builder.onStack(stack, 320); // exact size as original
                        
                        builder.putMat4f(this.mvInverse);
                        builder.putMat4f(this.projInverse);
                        builder.putMat4f(this.prevModelView);
                        builder.putMat4f(this.prevProjection);
                        
                        builder.putVec3(this.cameraPosX, this.cameraPosY, this.cameraPosZ);
                        builder.putVec3(this.prevCameraPosX, this.prevCameraPosY, this.prevCameraPosZ);
                        
                        builder.putVec2(this.viewResX, this.viewResY);
                        builder.putFloat(this.blendFactor);
                        builder.putFloat(this.inverseSamples);
                        builder.putFloat(this.handDepthThreshold);
                        
                        builder.putInt(this.motionBlurSamples);
                        builder.putInt(this.halfSamples);
                        builder.putInt(this.blurAlgorithm);
                        
                        ByteBuffer data = builder.get();
                        
                        GpuBuffer oldBuffer = uniformBuffers.get("MotionBlurParams");
                        if (oldBuffer != null) {
                            oldBuffer.close();
                        }
                        
                        GpuBuffer newBuffer = RenderSystem.getDevice().createBuffer(
                                () -> "FoxyClient MotionBlurParams UBO",
                                128, // UNIFORM BUFFER mode
                                data
                        );
                        
                        uniformBuffers.put("MotionBlurParams", newBuffer);
                    } catch (Throwable e) {
                        LOGGER.error("Failed to build uniform buffer", e);
                        throw e;
                    }
                }
            }
        }
    }

    // Setters
    public void setBlendFactor(float value) { this.blendFactor = value; }
    public void setViewRes(float x, float y) { this.viewResX = x; this.viewResY = y; }
    public void setMotionBlurSamples(int value) { this.motionBlurSamples = value; }
    public void setHalfSamples(int value) { this.halfSamples = value; }
    public void setInverseSamples(float value) { this.inverseSamples = value; }
    public void setBlurAlgorithm(int value) { this.blurAlgorithm = value; }
    public void setHandDepthThreshold(float value) { this.handDepthThreshold = value; }
    public void setMvInverse(Matrix4f mat) { this.mvInverse.set(mat); }
    public void setProjInverse(Matrix4f mat) { this.projInverse.set(mat); }
    public void setPrevModelView(Matrix4f mat) { this.prevModelView.set(mat); }
    public void setPrevProjection(Matrix4f mat) { this.prevProjection.set(mat); }
    public void setCameraPos(Vector3f pos) { this.cameraPosX = pos.x(); this.cameraPosY = pos.y(); this.cameraPosZ = pos.z(); }
    public void setPrevCameraPos(Vector3f pos) { this.prevCameraPosX = pos.x(); this.prevCameraPosY = pos.y(); this.prevCameraPosZ = pos.z(); }

    public void setFrameMotionBlur(Matrix4f modelView, Matrix4f prevModelView, Matrix4f projection, Matrix4f prevProjection, Vector3f cameraPos, Vector3f prevCameraPos) {
        this.mvInverse.set(modelView).invert();
        this.projInverse.set(projection).invert();
        this.prevModelView.set(prevModelView);
        this.prevProjection.set(prevProjection);
        
        this.cameraPosX = cameraPos.x();
        this.cameraPosY = cameraPos.y();
        this.cameraPosZ = cameraPos.z();
        
        this.prevCameraPosX = prevCameraPos.x();
        this.prevCameraPosY = prevCameraPos.y();
        this.prevCameraPosZ = prevCameraPos.z();
    }

    public void reload() {
        if (this.processor != null) {
            this.processor.close();
        }
        this.processor = null;
        this.initialized = false;
        this.errored = false;
    }

    public boolean isInitialized() {
        return this.initialized;
    }
}
