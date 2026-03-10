package com.foxyclient.util;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;

/**
 * Custom RenderLayers for FoxyClient.
 * Provides layers that bypass depth test for ESP.
 */
public class RenderLayers {
    private static final RenderPipeline BYPASS_TRANSLUCENT_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withLocation("foxyclient_bypass_translucent")
            .build()
    );

    private static final RenderLayer BYPASS_TRANSLUCENT = RenderLayer.of(
        "foxy_bypass_translucent",
        RenderSetup.builder(BYPASS_TRANSLUCENT_PIPELINE)
            .translucent()
            .build()
    );

    private static final RenderPipeline BYPASS_LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.TRANSFORMS_PROJECTION_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
            .withVertexShader("core/rendertype_lines")
            .withFragmentShader("core/rendertype_lines")
            .withBlend(com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.DrawMode.LINES)
            .withLocation("foxyclient_bypass_lines")
            .build()
    );

    private static final RenderLayer BYPASS_LINES = RenderLayer.of(
        "foxy_bypass_lines",
        RenderSetup.builder(BYPASS_LINES_PIPELINE)
            .layeringTransform(net.minecraft.client.render.LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .build()
    );

    public static RenderLayer getBypassTranslucent() { return BYPASS_TRANSLUCENT; }
    public static RenderLayer getBypassLines() { return net.minecraft.client.render.RenderLayers.LINES; }
    
    // For SoundESP compatibility
    public static RenderLayer lines() { return net.minecraft.client.render.RenderLayers.LINES; }
}
