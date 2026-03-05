package com.github.lumin.graphics.renderers;

import com.github.lumin.graphics.LuminRenderPipelines;
import com.github.lumin.graphics.LuminRenderSystem;
import com.github.lumin.graphics.buffer.LuminRingBuffer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class RoundRectRenderer implements IRenderer {
    private static final long BUFFER_SIZE = 2 * 1024 * 1024;
    private final LuminRingBuffer buffer = new LuminRingBuffer(BUFFER_SIZE, GpuBuffer.USAGE_VERTEX);

    private boolean scissorEnabled = false;
    private int scissorX, scissorY, scissorW, scissorH;
    private long currentOffset = 0;
    private int vertexCount = 0;

    public void addRoundRect(float x, float y, float width, float height, float radius, Color color) {
        addRoundRect(x, y, width, height, radius, radius, radius, radius, color);
    }

    public void addRoundRect(float x, float y, float width, float height, float rTL, float rTR, float rBR, float rBL, Color color) {
        buffer.tryMap();
        float x2 = x + width, y2 = y + height;
        int argb = color.getRGB();
        float radius = Math.max(Math.max(rTL, rTR), Math.max(rBR, rBL));
        float innerX1 = x + (Math.max(rTL, rBL) > 0 ? radius : 0);
        float innerY1 = y + (Math.max(rTL, rTR) > 0 ? radius : 0);
        float innerX2 = x2 - (Math.max(rTR, rBR) > 0 ? radius : 0);
        float innerY2 = y2 - (Math.max(rBL, rBR) > 0 ? radius : 0);

        addVertex(x, y, innerX1, innerY1, innerX2, innerY2, radius, argb);
        addVertex(x, y2, innerX1, innerY1, innerX2, innerY2, radius, argb);
        addVertex(x2, y2, innerX1, innerY1, innerX2, innerY2, radius, argb);
        addVertex(x2, y, innerX1, innerY1, innerX2, innerY2, radius, argb);
    }

    private void addVertex(float vx, float vy, float ix1, float iy1, float ix2, float iy2, float radius, int color) {
        long baseAddr = MemoryUtil.memAddress(buffer.getMappedBuffer());
        long p = baseAddr + currentOffset;
        MemoryUtil.memPutFloat(p, vx);
        MemoryUtil.memPutFloat(p + 4, vy);
        MemoryUtil.memPutFloat(p + 8, 0.0f);
        MemoryUtil.memPutInt(p + 12, ARGB.toABGR(color));
        MemoryUtil.memPutFloat(p + 16, ix1);
        MemoryUtil.memPutFloat(p + 20, iy1);
        MemoryUtil.memPutFloat(p + 24, ix2);
        MemoryUtil.memPutFloat(p + 28, iy2);
        MemoryUtil.memPutFloat(p + 32, radius);
        currentOffset += 36;
        vertexCount++;
    }

    @Override
    public void draw() {
        if (vertexCount == 0) return;
        if (buffer.isMapped()) buffer.unmap();

        LuminRenderSystem.QuadRenderingInfo info = LuminRenderSystem.prepareQuadRendering(vertexCount);
        if (info == null || info.target().getColorTextureView() == null) return;

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Round Rect Draw", info.target().getColorTextureView(), OptionalInt.empty(),
                info.target().getDepthTextureView(), OptionalDouble.empty())
        ) {
            pass.setPipeline(LuminRenderPipelines.ROUND_RECT);
            if (scissorEnabled) pass.enableScissor(scissorX, scissorY, scissorW, scissorH);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", info.dynamicUniforms());
            pass.setVertexBuffer(0, buffer.getGpuBuffer());
            pass.setIndexBuffer(info.ibo(), info.autoIndices().type());
            pass.drawIndexed(0, 0, info.indexCount(), 1);
        }
    }

    @Override
    public void clear() {
        if (vertexCount > 0) {
            if (buffer.isMapped()) buffer.unmap();
            buffer.rotate();
        }
        vertexCount = 0;
        currentOffset = 0;
    }

    @Override
    public void close() {
        buffer.close();
    }

    public void setScissor(int x, int y, int width, int height) {
        scissorEnabled = true;
        scissorX = x;
        scissorY = y;
        scissorW = width;
        scissorH = height;
    }

    public void clearScissor() {
        scissorEnabled = false;
    }
}