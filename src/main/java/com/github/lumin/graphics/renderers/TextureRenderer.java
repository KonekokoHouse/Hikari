package com.github.lumin.graphics.renderers;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.graphics.LuminRenderSystem;
import com.github.lumin.graphics.LuminTexture;
import com.github.lumin.graphics.buffer.LuminRingBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.io.InputStream;
import java.util.*;

public class TextureRenderer implements IRenderer {
    private final Minecraft mc = Minecraft.getInstance();

    private static final int STRIDE = 56; // 44 -> 56 because Radius is now vec4 (4 floats) instead of float (1 float). 3+1+2+4+4 = 14 floats * 4 = 56 bytes
    private final long bufferSize;
    private final Map<Object, Batch> batches = new LinkedHashMap<>();
    private final Map<Identifier, LuminTexture> textureCache = new HashMap<>();

    public TextureRenderer() {
        this(32 * 1024);
    }

    public TextureRenderer(long bufferSize) {
        this.bufferSize = bufferSize;
    }

    public void addQuadTexture(Identifier texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1, Color color) {
        addRoundedTexture(texture, x, y, width, height, 0f, u0, v0, u1, v1, color, false);
    }

    public void addQuadTexture(Identifier texture, float x, float y, float width, float height, float u0, float v0, float u1, float v1, Color color, boolean useLinearFilter) {
        addRoundedTexture(texture, x, y, width, height, 0f, u0, v0, u1, v1, color, useLinearFilter);
    }

    public void addRoundedTexture(Identifier texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, Color color) {
        addRoundedTexture(texture, x, y, width, height, radius, u0, v0, u1, v1, color, false);
    }

    public void addRoundedTexture(Identifier texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, Color color, boolean useLinearFilter) {
        addRoundedTexture((Object) texture, x, y, width, height, radius, radius, radius, radius, u0, v0, u1, v1, color, useLinearFilter);
    }

    public void addRoundedTexture(LuminTexture texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, Color color) {
        addRoundedTexture((Object) texture, x, y, width, height, radius, radius, radius, radius, u0, v0, u1, v1, color, true);
    }

    public void addRoundedTexture(Identifier texture, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBR, float radiusBL, float u0, float v0, float u1, float v1, Color color, boolean useLinearFilter) {
        addRoundedTexture((Object) texture, x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL, u0, v0, u1, v1, color, useLinearFilter);
    }

    public void addRoundedTexture(LuminTexture texture, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBR, float radiusBL, float u0, float v0, float u1, float v1, Color color) {
        addRoundedTexture((Object) texture, x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL, u0, v0, u1, v1, color, true);
    }

    private void addRoundedTexture(Object textureKey, float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, float u0, float v0, float u1, float v1, Color color, boolean useLinearFilter) {
        Batch batch = batches.computeIfAbsent(textureKey, k -> {
            Batch b = new Batch(new LuminRingBuffer(bufferSize, GpuBuffer.USAGE_VERTEX));
            b.useLinearFilter = useLinearFilter;
            return b;
        });

        batch.buffer.tryMap();

        if (batch.currentOffset + (long) STRIDE * 4L > bufferSize) {
            return;
        }

        int argb = ARGB.toABGR(color.getRGB());

        float x2 = x + width;
        float y2 = y + height;

        // Use maximum radius for inner rect calculation to ensure safe zone is correct
        // But with varying radii, the inner rect concept is tricky.
        // We will pass the OUTER Rect as InnerRect (as per shader modification plan)
        // and pass the radii vector.
        
        // Shader expects: InnerRect = Outer Bounds (x, y, x2, y2)
        // Shader expects: Radius = (TL, TR, BR, BL)
        
        float rectX1 = x;
        float rectY1 = y;
        float rectX2 = x2;
        float rectY2 = y2;

        long baseAddr = MemoryUtil.memAddress(batch.buffer.getMappedBuffer());
        long p = baseAddr + batch.currentOffset;

        writeVertex(p, x, y, u0, v0, argb, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        writeVertex(p + STRIDE, x, y2, u0, v1, argb, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        writeVertex(p + STRIDE * 2L, x2, y2, u1, v1, argb, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);
        writeVertex(p + STRIDE * 3L, x2, y, u1, v0, argb, rectX1, rectY1, rectX2, rectY2, rTL, rTR, rBR, rBL);

        batch.currentOffset += (long) STRIDE * 4L;
        batch.vertexCount += 4;
    }

    private void writeVertex(long addr, float x, float y, float u, float v, int color, float rx1, float ry1, float rx2, float ry2, float r1, float r2, float r3, float r4) {
        MemoryUtil.memPutFloat(addr, x);
        MemoryUtil.memPutFloat(addr + 4, y);
        MemoryUtil.memPutFloat(addr + 8, 0.0f); // z
        MemoryUtil.memPutInt(addr + 12, color);
        MemoryUtil.memPutFloat(addr + 16, u);
        MemoryUtil.memPutFloat(addr + 20, v);
        MemoryUtil.memPutFloat(addr + 24, rx1);
        MemoryUtil.memPutFloat(addr + 28, ry1);
        MemoryUtil.memPutFloat(addr + 32, rx2);
        MemoryUtil.memPutFloat(addr + 36, ry2);
        // Radius vector (TL, TR, BR, BL)
        MemoryUtil.memPutFloat(addr + 40, r1);
        MemoryUtil.memPutFloat(addr + 44, r2);
        MemoryUtil.memPutFloat(addr + 48, r3);
        MemoryUtil.memPutFloat(addr + 52, r4);
    }

    @Override
    public void draw() {
        if (batches.isEmpty()) return;

        LuminRenderSystem.applyOrthoProjection();

        var target = Minecraft.getInstance().getMainRenderTarget();
        if (target.getColorTextureView() == null) return;

        GpuBufferSlice dynamicUniforms = RenderSystem.getDynamicUniforms().writeTransform(
                RenderSystem.getModelViewMatrix(),
                new Vector4f(1, 1, 1, 1),
                new Vector3f(0, 0, 0),
                TextureTransform.DEFAULT_TEXTURING.getMatrix()
        );

        for (Map.Entry<Object, Batch> entry : batches.entrySet()) {
            Object textureKey = entry.getKey();
            Batch batch = entry.getValue();
            if (batch.vertexCount == 0) continue;

            if (batch.buffer.isMapped()) {
                batch.buffer.unmap();
            }

            int indexCount = (batch.vertexCount / 4) * 6;
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
            GpuBuffer ibo = autoIndices.getBuffer(indexCount);

            LuminTexture texture;
            if (textureKey instanceof Identifier id) {
                texture = textureCache.computeIfAbsent(id, key -> loadTexture(key, batch.useLinearFilter));
            } else if (textureKey instanceof LuminTexture tex) {
                texture = tex;
            } else {
                continue;
            }

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    () -> "Rounded Texture Draw",
                    target.getColorTextureView(), OptionalInt.empty(),
                    null, OptionalDouble.empty())
            ) {
                pass.setPipeline(LuminRenderPipelines.TEXTURE);

                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", dynamicUniforms);

                // 使用 RingBuffer 当前指向的 GpuBuffer
                pass.setVertexBuffer(0, batch.buffer.getGpuBuffer());
                pass.setIndexBuffer(ibo, autoIndices.type());
                pass.bindTexture("Sampler0", texture.textureView(), texture.sampler());

                pass.drawIndexed(0, 0, indexCount, 1);
            }
        }
    }

    private LuminTexture loadTexture(Identifier identifier, boolean useLinearFilter) {
        NativeImage image = null;
        try {
            Optional<Resource> resource = mc.getResourceManager().getResource(identifier);
            if (resource.isPresent()) {
                try (InputStream is = resource.get().open()) {
                    image = NativeImage.read(is);
                }
            }
        } catch (Exception ignored) {
        }

        if (image == null) {
            image = MissingTextureAtlasSprite.generateMissingImage();
        }

        try (NativeImage finalImage = image) {
            GpuTexture texture = RenderSystem.getDevice().createTexture(
                    () -> "Lumin-Texture: " + identifier,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    TextureFormat.RGBA8,
                    finalImage.getWidth(),
                    finalImage.getHeight(),
                    1,
                    1
            );

            var view = RenderSystem.getDevice().createTextureView(texture);
            FilterMode filterMode = useLinearFilter ? FilterMode.LINEAR : FilterMode.NEAREST;
            var sampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    filterMode,
                    filterMode,
                    1,
                    OptionalDouble.empty()
            );

            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, finalImage);
            return new LuminTexture(texture, view, sampler);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load texture " + identifier, e);
        }
    }

    @Override
    public void clear() {
        for (Batch batch : batches.values()) {
            // 2. 轮转缓冲区：只有确实绘制过数据才 rotate
            if (batch.vertexCount > 0) {
                if (batch.buffer.isMapped()) {
                    batch.buffer.unmap();
                }
                batch.buffer.rotate();
            }
            batch.currentOffset = 0;
            batch.vertexCount = 0;
        }
    }

    @Override
    public void close() {
        clear();
        for (Batch batch : batches.values()) {
            batch.buffer.close();
        }
        batches.clear();
        for (LuminTexture texture : textureCache.values()) {
            texture.close();
        }
        textureCache.clear();
    }

    private static final class Batch {
        final LuminRingBuffer buffer;
        long currentOffset = 0;
        int vertexCount = 0;
        boolean useLinearFilter;

        private Batch(LuminRingBuffer buffer) {
            this.buffer = buffer;
        }
    }
}