package com.github.lumin.graphics.renderers;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.graphics.LuminRenderSystem;
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
import com.mojang.blaze3d.ProjectionType;
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

public class BlurRenderer implements AutoCloseable {

    private static BlurRenderer INSTANCE;

    public static BlurRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new BlurRenderer();
        }
        return INSTANCE;
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final List<RenderTarget> targets = new ArrayList<>();
    private RenderTarget outputTarget;
    private GpuBuffer quadBuffer;
    private GpuBuffer uniformBuffer;
    private GpuBuffer projectionBuffer;
    private GpuSampler linearSampler;
    private int lastWidth, lastHeight;

    public BlurRenderer() {
        initBuffers();
    }

    private void initBuffers() {
        // Init Quad Buffer (Position + TexCoord)
        // 4 vertices * 20 bytes (3 floats pos + 2 floats tex)
        ByteBuffer buffer = MemoryUtil.memAlloc(80);

        // Vertex 0: -1, -1, 0, 0, 0
        buffer.putFloat(-1f).putFloat(-1f).putFloat(0f).putFloat(0f).putFloat(0f);
        // Vertex 1: 1, -1, 0, 1, 0
        buffer.putFloat(1f).putFloat(-1f).putFloat(0f).putFloat(1f).putFloat(0f);
        // Vertex 2: 1, 1, 0, 1, 1
        buffer.putFloat(1f).putFloat(1f).putFloat(0f).putFloat(1f).putFloat(1f);
        // Vertex 3: -1, 1, 0, 0, 1
        buffer.putFloat(-1f).putFloat(1f).putFloat(0f).putFloat(0f).putFloat(1f);

        buffer.flip();

        quadBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Blur Quad",
                GpuBuffer.USAGE_VERTEX,
                buffer
        );
        MemoryUtil.memFree(buffer);

        // Init Uniform Buffer (32 bytes)
        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Blur Info",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                32
        );

        // Init Projection Buffer (Identity)
        projectionBuffer = RenderSystem.getDevice().createBuffer(
                () -> "Blur Projection",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE,
                RenderSystem.PROJECTION_MATRIX_UBO_SIZE
        );

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

        // Clear old targets
        for (RenderTarget target : targets) {
            target.destroyBuffers();
        }
        targets.clear();
        if (outputTarget != null) {
            outputTarget.destroyBuffers();
            outputTarget = null;
        }

        // Create new targets for downsampling
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

    public RenderTarget blur(float radius, boolean outputToScreen) {
        if (radius <= 0) return null;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        resize(width, height);

        if (targets.isEmpty()) return null;

        // Determine number of passes based on radius
        int passes = Mth.clamp((int) radius, 1, targets.size());

        RenderTarget mainTarget = mc.getMainRenderTarget();

        // Backup projection
        RenderSystem.backupProjectionMatrix();
        // Set identity projection
        // We write identity to our projection buffer and bind it
        try (var pointer = RenderSystem.getDevice().createCommandEncoder().mapBuffer(projectionBuffer, false, true)) {
            ByteBuffer buf = pointer.data();
            for(int i=0; i<16; i++) {
                buf.putFloat(i % 5 == 0 ? 1.0f : 0.0f);
            }
        }
        RenderSystem.setProjectionMatrix(projectionBuffer.slice(), ProjectionType.ORTHOGRAPHIC);

        // 1. Downsample passes
        RenderTarget source = mainTarget;
        for (int i = 0; i < passes; i++) {
            RenderTarget dest = targets.get(i);
            renderPass(source, dest, i, true, radius);
            source = dest;
        }

        // 2. Upsample passes
        // If outputToScreen is false, we render the last pass to outputTarget instead of mainTarget
        for (int i = passes - 1; i >= 0; i--) {
            RenderTarget dest;
            if (i == 0) {
                dest = outputToScreen ? mainTarget : outputTarget;
            } else {
                dest = targets.get(i - 1);
            }
            renderPass(source, dest, i, false, radius);
            source = dest;
        }

        RenderSystem.restoreProjectionMatrix();


        return outputToScreen ? null : outputTarget;
    }

    public void blur(float radius) {
        blur(radius, true);
    }

    public void drawBlurRect(float x, float y, float width, float height, float radius, float blurRadius) {
        drawBlurRect(x, y, width, height, radius, radius, radius, radius, blurRadius);
    }

    public void drawBlurRect(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float blurRadius) {
        if (blurRadius <= 0) return;

        RenderTarget blurred = blur(blurRadius, false);
        if (blurred == null) return;

        int screenW = mc.getWindow().getWidth();
        int screenH = mc.getWindow().getHeight();

        float guiScaleFactor = (float) mc.getWindow().getGuiScale();

        float realX = x * guiScaleFactor;
        float realY = y * guiScaleFactor;
        float realW = width * guiScaleFactor;
        float realH = height * guiScaleFactor;

        float u0 = realX / screenW;
        float v0 = (realY + realH) / screenH; // Flip V
        float u1 = (realX + realW) / screenW;
        float v1 = realY / screenH; // Flip V

        // Vertex Format in TextureRenderer:
        // Position (3) + Color (1) + UV (2) + InnerRect (4) + Radius (4) = 14 floats = 56 bytes
        // 4 vertices = 224 bytes.

        // Wait, TextureRenderer uses LuminVertexFormats.TEXTURE.
        // We should reuse that pipeline if we want rounded corners.

        // Calculate inner rect for rounded corners
        ByteBuffer buf = MemoryUtil.memAlloc(224);

        float x2 = x + width;
        float y2 = y + height;
        float rectX1 = x;
        float rectY1 = y;
        float rectX2 = x2;
        float rectY2 = y2;

        int color = 0xFFFFFFFF; // White

        // Vertex 0: x, y
        writeVertex(buf, x, y, u0, v0, color, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        // Vertex 1: x, y2
        writeVertex(buf, x, y2, u0, v1, color, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        // Vertex 2: x2, y2
        writeVertex(buf, x2, y2, u1, v1, color, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        // Vertex 3: x2, y
        writeVertex(buf, x2, y, u1, v0, color, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);

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

    private void renderPass(RenderTarget source, RenderTarget dest, int level, boolean isDown, float totalRadius) {
        float w = source.width;
        float h = source.height;
        float offset = 1.0f + (totalRadius * 0.2f); // Adjusted scaling

        try (var pointer = RenderSystem.getDevice().createCommandEncoder().mapBuffer(uniformBuffer, false, true)) {
            ByteBuffer buf = pointer.data();
            // uHalfTexelSize (0.5 / width, 0.5 / height)
            buf.putFloat(0.5f / w).putFloat(0.5f / h);
            // uOffset
            buf.putFloat(offset);
            // padding
            buf.putFloat(0f);
            // uUvScale (1, 1)
            buf.putFloat(1f).putFloat(1f);
            // uUvOffset (0, 0)
            buf.putFloat(0f).putFloat(0f);
        }

        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                new Matrix4f(), // Identity ModelView
                new Vector4f(1, 1, 1, 1),
                new Vector3f(0, 0, 0),
                new Matrix4f() // Identity Texture
        );

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Blur Pass " + (isDown ? "Down" : "Up") + " " + level,
                dest.getColorTextureView(), OptionalInt.of(0),
                null, OptionalDouble.empty())
        ) {
            pass.setPipeline(isDown ? LuminRenderPipelines.BLUR_DOWN : LuminRenderPipelines.BLUR_UP);

            pass.setUniform("DynamicTransforms", dynamicUniforms);
            pass.setUniform("BlurInfo", uniformBuffer);
            pass.setUniform("Projection", projectionBuffer);

            pass.setVertexBuffer(0, quadBuffer);

            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer ibo = autoIndices.getBuffer(6);
            pass.setIndexBuffer(ibo, autoIndices.type());

            pass.bindTexture("Sampler0", source.getColorTextureView(), linearSampler);

            pass.drawIndexed(0, 0, 6, 1);
        }
    }

    @Override
    public void close() {
        if (quadBuffer != null) quadBuffer.close();
        if (uniformBuffer != null) uniformBuffer.close();
        if (projectionBuffer != null) projectionBuffer.close();
        if (outputTarget != null) outputTarget.destroyBuffers();
        for (RenderTarget target : targets) {
            target.destroyBuffers();
        }
        targets.clear();
    }
}
