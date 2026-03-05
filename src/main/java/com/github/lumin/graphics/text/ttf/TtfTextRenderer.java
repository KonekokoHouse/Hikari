package com.github.lumin.graphics.text.ttf;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.graphics.LuminRenderSystem;
import com.github.lumin.graphics.buffer.BufferUtils;
import com.github.lumin.graphics.buffer.LuminRingBuffer;
import com.github.lumin.graphics.text.GlyphDescriptor;
import com.github.lumin.graphics.text.ITextRenderer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class TtfTextRenderer implements ITextRenderer {

    private static final float DEFAULT_SCALE = 0.30f;
    private static final float SPACING = 1f;
    private static final int STRIDE = 24;
    private final long bufferSize;

    private final Map<TtfGlyphAtlas, Batch> batches = new LinkedHashMap<>();

    private GpuBuffer ttfInfoUniformBuf = null;

    private boolean scissorEnabled = false;
    private int scissorX, scissorY, scissorW, scissorH;

    public TtfTextRenderer(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public TtfTextRenderer() {
        this(2 * 1024 * 1024);
    }

    @Override
    public void addText(String text, float x, float y, float scale, Color color, TtfFontLoader fontLoader) {
        final var finalScale = scale * DEFAULT_SCALE;
        fontLoader.checkAndLoadChars(text);
        int argb = ARGB.toABGR(color.getRGB());

        float xOffset = 0f;
        float yOffset = 0f;

        for (char ch : text.toCharArray()) {
            if (ch == ' ') {
                xOffset += 3.0f * scale;
                continue;
            }
            if (ch == '\n') {
                xOffset = 0f;
                yOffset += fontLoader.fontFile.fontHeight * finalScale;
                continue;
            }

            GlyphDescriptor glyph = fontLoader.getGlyph(ch);
            if (glyph == null) continue;

            TtfGlyphAtlas atlas = glyph.atlas();

            Batch batch = batches.computeIfAbsent(atlas,
                    k -> new Batch(new LuminRingBuffer(bufferSize, GpuBuffer.USAGE_VERTEX)));

            batch.buffer.tryMap();

            float baselineY = yOffset + y + (fontLoader.fontFile.pixelAscent * finalScale);
            float x1 = x + xOffset;
            float x2 = x1 + glyph.width() * finalScale;
            float y1 = baselineY + glyph.yOffset() * finalScale;
            float y2 = y1 + glyph.height() * finalScale;

            long baseAddr = MemoryUtil.memAddress(batch.buffer.getMappedBuffer());
            long p = baseAddr + batch.offsetInAtlas;

            BufferUtils.writeUvRectToAddr(p, x1, y1, glyph.uv().u0(), glyph.uv().v0(), argb);
            BufferUtils.writeUvRectToAddr(p + STRIDE, x1, y2, glyph.uv().u0(), glyph.uv().v1(), argb);
            BufferUtils.writeUvRectToAddr(p + STRIDE * 2, x2, y2, glyph.uv().u1(), glyph.uv().v1(), argb);
            BufferUtils.writeUvRectToAddr(p + STRIDE * 3, x2, y1, glyph.uv().u1(), glyph.uv().v0(), argb);

            batch.offsetInAtlas += (STRIDE * 4);
            xOffset += glyph.advance() * finalScale + SPACING * scale;
        }
    }

    @Override
    public void draw() {
        if (batches.isEmpty()) return;

        LuminRenderSystem.applyOrthoProjection();

        if (ttfInfoUniformBuf == null) {
            final var size = new Std140SizeCalculator().putFloat().get();
            ttfInfoUniformBuf = RenderSystem.getDevice().createBuffer(
                    () -> "Lumin TTF UBO", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, size);

            try (GpuBuffer.MappedView mappedView = RenderSystem.getDevice().createCommandEncoder()
                    .mapBuffer(ttfInfoUniformBuf, false, true)) {
                Std140Builder.intoBuffer(mappedView.data()).putFloat(0.475f);
            }
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.getColorTextureView() == null) return;

        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(), new Vector4f(1, 1, 1, 1),
                new Vector3f(0, 0, 0), TextureTransform.DEFAULT_TEXTURING.getMatrix()
        );

        for (Map.Entry<TtfGlyphAtlas, Batch> entry : batches.entrySet()) {
            final var atlas = entry.getKey();
            final var batch = entry.getValue();

            if (batch.offsetInAtlas == 0) continue;

            if (batch.buffer.isMapped()) {
                batch.buffer.unmap();
            }

            int vertexCount = (int) (batch.offsetInAtlas / STRIDE);
            int indexCount = (vertexCount / 4) * 6;

            RenderSystem.AutoStorageIndexBuffer autoIndices =
                    RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer ibo = autoIndices.getBuffer(indexCount);

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Lumin TTF Draw",
                    target.getColorTextureView(), OptionalInt.empty(),
                    target.getDepthTextureView(), OptionalDouble.empty())
            ) {
                pass.setPipeline(LuminRenderPipelines.TTF_FONT);
                if (scissorEnabled) {
                    pass.enableScissor(scissorX, scissorY, scissorW, scissorH);
                }

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicUniforms);
                pass.setUniform("TtfInfo", ttfInfoUniformBuf);

                pass.setVertexBuffer(0, batch.buffer.getGpuBuffer());
                pass.setIndexBuffer(ibo, autoIndices.type());
                pass.bindTexture("Sampler0", atlas.getTexture().textureView(), atlas.getTexture().sampler());

                pass.drawIndexed(0, 0, indexCount, 1);
            }
        }
    }

    @Override
    public void clear() {
        for (Batch batch : batches.values()) {
            if (batch.offsetInAtlas > 0) {
                if (batch.buffer.isMapped()) {
                    batch.buffer.unmap();
                }
                batch.buffer.rotate();
            }
            batch.offsetInAtlas = 0;
        }
    }

    @Override
    public void close() {
        clear();
        for (Batch batch : batches.values()) {
            batch.buffer.close();
        }
        batches.clear();
        if (ttfInfoUniformBuf != null) ttfInfoUniformBuf.close();
    }

    @Override
    public float getHeight(float scale, TtfFontLoader fontLoader) {
        return fontLoader.fontFile.pixelAscent * DEFAULT_SCALE * scale;
    }

    @Override
    public float getWidth(String text, float scale, TtfFontLoader fontLoader) {
        fontLoader.checkAndLoadChars(text);
        final var finalScale = scale * DEFAULT_SCALE;
        float maxLine = 0.0f;
        float currentLine = 0.0f;

        for (char ch : text.toCharArray()) {
            if (ch == ' ') {
                currentLine += 3.0f * scale;
            } else if (ch == '\n') {
                maxLine = Math.max(maxLine, currentLine);
                currentLine = 0.0f;
            } else {
                GlyphDescriptor glyph = fontLoader.getGlyph(ch);
                if (glyph != null) {
                    currentLine += glyph.advance() * finalScale + SPACING * scale;
                }
            }
        }
        return Math.max(maxLine, currentLine);
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        scissorEnabled = true;
        scissorX = x;
        scissorY = y;
        scissorW = width;
        scissorH = height;
    }

    @Override
    public void clearScissor() {
        scissorEnabled = false;
    }

    private static final class Batch {
        final LuminRingBuffer buffer;
        long offsetInAtlas = 0;

        private Batch(LuminRingBuffer buffer) {
            this.buffer = buffer;
        }
    }
}