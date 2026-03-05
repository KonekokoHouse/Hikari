package com.github.lumin.graphics;

import com.github.lumin.utils.resources.ResourceLocationUtils;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;


public class LuminRenderPipelines {

    public final static RenderPipeline RECTANGLE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/rectangle"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("rectangle"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public final static RenderPipeline LINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/line"))
            .withVertexFormat(LuminVertexFormats.LINE, VertexFormat.Mode.LINES)
            .withVertexShader(ResourceLocationUtils.getIdentifier("line"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("line"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    private final static RenderPipeline.Snippet TTF_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withUniform("TtfInfo", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public final static RenderPipeline TTF_FONT = RenderPipeline.builder(TTF_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/ttf_font"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("ttf_font"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public final static RenderPipeline ROUND_RECT = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/round_rectangle"))
            .withVertexFormat(LuminVertexFormats.ROUND_RECT, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("round_rectangle"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("round_rectangle"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public final static RenderPipeline TEXTURE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/texture"))
            .withVertexFormat(LuminVertexFormats.TEXTURE, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("texture"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("texture"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    private final static RenderPipeline.Snippet BLUR_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withUniform("BlurInfo", UniformType.UNIFORM_BUFFER)
            .buildSnippet();

    public final static RenderPipeline BLUR_DOWN = RenderPipeline.builder(BLUR_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/blur_down"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("blur"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("blur_down"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public final static RenderPipeline BLUR_UP = RenderPipeline.builder(BLUR_SNIPPET)
            .withLocation(ResourceLocationUtils.getIdentifier("pipelines/blur_up"))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
            .withVertexShader(ResourceLocationUtils.getIdentifier("blur"))
            .withFragmentShader(ResourceLocationUtils.getIdentifier("blur_up"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withSampler("Sampler0")
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .build();

    public static void onRegisterRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(RECTANGLE);
        event.registerPipeline(TTF_FONT);
        event.registerPipeline(LINE);
        event.registerPipeline(ROUND_RECT);
        event.registerPipeline(TEXTURE);
        event.registerPipeline(BLUR_DOWN);
        event.registerPipeline(BLUR_UP);
    }

}
