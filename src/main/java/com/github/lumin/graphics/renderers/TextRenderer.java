package com.github.lumin.graphics.renderers;

import com.github.lumin.graphics.LuminRenderSystem;
import com.github.lumin.graphics.text.ITextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.graphics.text.ttf.TtfFontLoader;
import com.github.lumin.graphics.text.ttf.TtfTextRenderer;

import java.awt.*;

public class TextRenderer implements IRenderer {

    private final ITextRenderer textRenderer;

    public TextRenderer(long bufferSize) {
        textRenderer = new TtfTextRenderer(bufferSize);
    }

    public TextRenderer() {
        textRenderer = new TtfTextRenderer();
    }

    public void addText(String text, float x, float y, float scale, Color color, TtfFontLoader fontLoader) {
        textRenderer.addText(text, x, y, scale, color, fontLoader);
    }

    public void addText(String text, float x, float y, float scale, Color color) {
        textRenderer.addText(text, x, y, scale, color, StaticFontLoader.DEFAULT);
    }

    public void addText(String text, float x, float y, Color color, TtfFontLoader fontLoader) {
        textRenderer.addText(text, x, y, 1.0f, color, fontLoader);
    }

    public void addText(String text, float x, float y, Color color) {
        textRenderer.addText(text, x, y, 1.0f, color, StaticFontLoader.DEFAULT);
    }

    public float getHeight(float scale) {
        return textRenderer.getHeight(scale, StaticFontLoader.DEFAULT);
    }

    public float getHeight(float scale, TtfFontLoader fontLoader) {
        return textRenderer.getHeight(scale, fontLoader);
    }

    public float getWidth(String text, float scale) {
        return textRenderer.getWidth(text, scale, StaticFontLoader.DEFAULT);
    }

    public float getWidth(String text, float scale, TtfFontLoader fontLoader) {
        return textRenderer.getWidth(text, scale, fontLoader);
    }

    public void setScissor(int x, int y, int width, int height) {
        textRenderer.setScissor(x, y, width, height);
    }

    public void clearScissor() {
        textRenderer.clearScissor();
    }

    @Override
    public void draw() {
        LuminRenderSystem.applyOrthoProjection();
        textRenderer.draw();
    }

    @Override
    public void clear() {
        textRenderer.clear();
    }

    @Override
    public void close() {
        textRenderer.close();
    }
}
