package com.github.lumin.graphics.shaders;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.graphics.LuminRenderSystem;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class BlurShader implements AutoCloseable {

    public static final BlurShader INSTANCE = new BlurShader();

    private final Minecraft mc = Minecraft.getInstance();
    private final List<RenderTarget> targets = new ArrayList<>();
    private RenderTarget outputTarget;
    private GpuBuffer quadBuffer;
    private GpuSampler linearSampler;
    private int lastWidth, lastHeight;

    public BlurShader() {
        initBuffers();
    }

    private void initBuffers() {
        ByteBuffer buffer = MemoryUtil.memAlloc(80);

        buffer.putFloat(-1f).putFloat(-1f).putFloat(0f).putFloat(0f).putFloat(0f);
        buffer.putFloat(1f).putFloat(-1f).putFloat(0f).putFloat(1f).putFloat(0f);
        buffer.putFloat(1f).putFloat(1f).putFloat(0f).putFloat(1f).putFloat(1f);
        buffer.putFloat(-1f).putFloat(1f).putFloat(0f).putFloat(0f).putFloat(1f);

        buffer.flip();

        quadBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Lumin Blur",
                GpuBuffer.USAGE_VERTEX,
                buffer
        );
        MemoryUtil.memFree(buffer);

        linearSampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE,
                AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR,
                FilterMode.LINEAR,
                1, OptionalDouble.empty()
        );
    }

    public void resize(int width, int height) {
        if (width == lastWidth && height == lastHeight) return;
        lastWidth = width;
        lastHeight = height;

        for (RenderTarget target : targets) {
            target.destroyBuffers();
        }
        targets.clear();
        if (outputTarget != null) {
            outputTarget.destroyBuffers();
            outputTarget = null;
        }

        int currentW = width;
        int currentH = height;
        for (int i = 0; i < 5; i++) {
            currentW /= 2;
            currentH /= 2;
            if (currentW <= 0 || currentH <= 0) break;

            RenderTarget target = new TextureTarget("Blur Target " + i, currentW, currentH, false, false);
            targets.add(target);
        }

        outputTarget = new TextureTarget("Blur Output", width, height, false, false);
    }

    public RenderTarget blur(float radius) {
        if (radius <= 0) return null;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        resize(width, height);

        if (targets.isEmpty()) return null;

        // Strategy:
        // Pass 1: radius 0-4
        // Pass 2: radius 4-8
        // Pass 3: radius 8-12
        float passInterval = 4.0f;
        int passes = Mth.clamp((int) Math.ceil(radius / passInterval), 1, targets.size());

        float offset = Math.max(1.0f, radius / passes);

        RenderTarget mainTarget = mc.getMainRenderTarget();

        RenderSystem.backupProjectionMatrix();

        ByteBuffer projBuf = MemoryUtil.memAlloc(RenderSystem.PROJECTION_MATRIX_UBO_SIZE);
        for (int i = 0; i < 16; i++) {
            projBuf.putFloat(i % 5 == 0 ? 1.0f : 0.0f);
        }
        projBuf.flip();

        try (GpuBuffer projectionBuffer = RenderSystem.getDevice().createBuffer(() -> "Blur Projection", GpuBuffer.USAGE_UNIFORM, projBuf)) {
            MemoryUtil.memFree(projBuf);
            GpuBufferSlice projectionSlice = projectionBuffer.slice();

            RenderSystem.setProjectionMatrix(projectionSlice, ProjectionType.ORTHOGRAPHIC);

            // 1. Downsample passes
            RenderTarget source = mainTarget;
            for (int i = 0; i < passes; i++) {
                RenderTarget dest = targets.get(i);
                renderPass(source, dest, i, true, offset, projectionSlice);
                source = dest;
            }

            // 2. Upsample passes
            for (int i = passes - 1; i >= 0; i--) {
                RenderTarget dest;
                if (i == 0) {
                    dest = outputTarget;
                } else {
                    dest = targets.get(i - 1);
                }
                renderPass(source, dest, i, false, offset, projectionSlice);
                source = dest;
            }
        }

        RenderSystem.restoreProjectionMatrix();


        return outputTarget;
    }

    public void drawBlur(float x, float y, float width, float height, float radius, float blurRadius) {
        drawBlur(x, y, width, height, radius, radius, radius, radius, blurRadius);
    }

    public void drawBlur(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float blurRadius) {
        if (blurRadius <= 0) return;

        RenderTarget blurred = blur(blurRadius);
        if (blurred == null) return;

        int screenW = mc.getWindow().getWidth();
        int screenH = mc.getWindow().getHeight();

        float guiScaleFactor = (float) mc.getWindow().getGuiScale();

        float realX = x * guiScaleFactor;
        float realY = y * guiScaleFactor;
        float realW = width * guiScaleFactor;
        float realH = height * guiScaleFactor;

        float u0 = realX / screenW;
        float v0 = (realY + realH) / screenH;
        float u1 = (realX + realW) / screenW;
        float v1 = realY / screenH;

        ByteBuffer buf = MemoryUtil.memAlloc(224);

        float x2 = x + width;
        float y2 = y + height;

        int color = 0xFFFFFFFF; // White

        // Vertex 0: x, y
        writeVertex(buf, x, y, u0, v0, color, x, y, x2, y2, rTL, rTR, rBR, rBL);
        // Vertex 1: x, y2
        writeVertex(buf, x, y2, u0, v1, color, x, y, x2, y2, rTL, rTR, rBR, rBL);
        // Vertex 2: x2, y2
        writeVertex(buf, x2, y2, u1, v1, color, x, y, x2, y2, rTL, rTR, rBR, rBL);
        // Vertex 3: x2, y
        writeVertex(buf, x2, y, u1, v0, color, x, y, x2, y2, rTL, rTR, rBR, rBL);

        buf.flip();

        try (GpuBuffer drawBuffer = RenderSystem.getDevice().createBuffer(() -> "Blur Rect", GpuBuffer.USAGE_VERTEX, buf)) {
            MemoryUtil.memFree(buf);

            // Draw
            LuminRenderSystem.applyOrthoProjection();
            RenderTarget mainTarget = mc.getMainRenderTarget();

            GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                    RenderSystem.getModelViewMatrix(),
                    new Vector4f(1, 1, 1, 1),
                    new Vector3f(0, 0, 0),
                    TextureTransform.DEFAULT_TEXTURING.getMatrix()
            );

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Blur Rect Draw",
                    mainTarget.getColorTextureView(), OptionalInt.empty(),
                    mainTarget.getDepthTextureView(), OptionalDouble.empty())
            ) {
                pass.setPipeline(LuminRenderPipelines.TEXTURE);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicUniforms);

                pass.setVertexBuffer(0, drawBuffer);

                RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer ibo = autoIndices.getBuffer(6);
                pass.setIndexBuffer(ibo, autoIndices.type());

                pass.bindTexture("Sampler0", blurred.getColorTextureView(), linearSampler);

                pass.drawIndexed(0, 0, 6, 1);
            }
        }
    }

    private void writeVertex(ByteBuffer buf, float x, float y, float u, float v, int color, float rx1, float ry1, float rx2, float ry2, float r1, float r2, float r3, float r4) {
        buf.putFloat(x);
        buf.putFloat(y);
        buf.putFloat(0.0f); // z
        buf.putInt(color);
        buf.putFloat(u);
        buf.putFloat(v);
        buf.putFloat(rx1);
        buf.putFloat(ry1);
        buf.putFloat(rx2);
        buf.putFloat(ry2);
        buf.putFloat(r1);
        buf.putFloat(r2);
        buf.putFloat(r3);
        buf.putFloat(r4);
    }

    private void renderPass(RenderTarget source, RenderTarget dest, int level, boolean isDown, float offset, GpuBufferSlice projectionSlice) {
        float w = source.width;
        float h = source.height;

        ByteBuffer uniformBuf = MemoryUtil.memAlloc(32);
        // uHalfTexelSize (0.5 / width, 0.5 / height)
        uniformBuf.putFloat(0.5f / w).putFloat(0.5f / h);
        // uOffset
        uniformBuf.putFloat(offset);
        // padding
        uniformBuf.putFloat(0f);
        // uUvScale (1, 1)
        uniformBuf.putFloat(1f).putFloat(1f);
        // uUvOffset (0, 0)
        uniformBuf.putFloat(0f).putFloat(0f);
        uniformBuf.flip();

        try (GpuBuffer uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Blur Info", GpuBuffer.USAGE_UNIFORM, uniformBuf)) {
            MemoryUtil.memFree(uniformBuf);

            GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                    new Matrix4f(),
                    new Vector4f(1, 1, 1, 1),
                    new Vector3f(0, 0, 0),
                    new Matrix4f()
            );

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Blur Pass " + (isDown ? "Down" : "Up") + " " + level,
                    dest.getColorTextureView(), OptionalInt.of(0),
                    null, OptionalDouble.empty())
            ) {
                pass.setPipeline(isDown ? LuminRenderPipelines.BLUR_DOWN : LuminRenderPipelines.BLUR_UP);

                pass.setUniform("DynamicTransforms", dynamicUniforms);
                pass.setUniform("BlurInfo", uniformBuffer.slice());
                pass.setUniform("Projection", projectionSlice);

                pass.setVertexBuffer(0, quadBuffer);

                RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer ibo = autoIndices.getBuffer(6);
                pass.setIndexBuffer(ibo, autoIndices.type());

                pass.bindTexture("Sampler0", source.getColorTextureView(), linearSampler);

                pass.drawIndexed(0, 0, 6, 1);
            }
        }
    }

    @Override
    public void close() {
        if (quadBuffer != null) quadBuffer.close();
        if (outputTarget != null) outputTarget.destroyBuffers();
        for (RenderTarget target : targets) {
            target.destroyBuffers();
        }
        targets.clear();
    }

}
