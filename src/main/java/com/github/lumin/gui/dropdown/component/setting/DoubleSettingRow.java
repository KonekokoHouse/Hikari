package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.awt.*;

public class DoubleSettingRow extends SettingRow<DoubleSetting> {

    private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private final Animation pressAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation indicatorAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
    private final TextRenderer measureTextRenderer = new TextRenderer();
    private boolean dragging;
    private boolean focused;
    private String inputBuffer;
    private int cursorIndex;

    public DoubleSettingRow(DoubleSetting setting) {
        super(setting);
        hoverAnimation.setStartValue(0.0f);
        pressAnimation.setStartValue(0.0f);
        indicatorAnimation.setStartValue(0.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        hoverAnimation.run(dragging ? 1.0f : hoverProgress);
        pressAnimation.run(dragging ? 1.0f : 0.0f);
        indicatorAnimation.run((dragging || hoverProgress > 0.01f) ? 1.0f : 0.0f);

        float animatedHover = hoverAnimation.getValue();
        float animatedPress = pressAnimation.getValue();
        float indicatorProgress = indicatorAnimation.getValue();

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, animatedHover));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, labelY, labelScale, DropdownTheme.TEXT_PRIMARY);

        DropdownLayout.Rect trackBounds = getTrackBounds(bounds);
        DropdownLayout.Rect fieldBounds = getFieldBounds(bounds);
        float progress = getProgress();
        float handleWidth = 4.0f - animatedPress * 2.0f;
        float handleHeight = 14.0f;
        float handleX = trackBounds.x() + trackBounds.width() * progress - handleWidth / 2.0f;
        float handleY = trackBounds.centerY() - handleHeight / 2.0f;
        float handleGap = 4.0f;
        float activeWidth = Math.max(2.0f, trackBounds.width() * progress - handleWidth / 2.0f - handleGap);
        float inactiveX = handleX + handleWidth + handleGap;
        float inactiveWidth = Math.max(2.0f, trackBounds.right() - inactiveX);

        if (shouldDrawSteps()) {
            drawSteps(rectRenderer, trackBounds, progress);
        }

        roundRectRenderer.addRoundRect(trackBounds.x(), trackBounds.y(), activeWidth, trackBounds.height(), 3.0f, 0.0f, 0.0f, 3.0f, DropdownTheme.PRIMARY);
        roundRectRenderer.addRoundRect(inactiveX, trackBounds.y(), inactiveWidth, trackBounds.height(), 0.0f, 3.0f, 3.0f, 0.0f, DropdownTheme.SECONDARY_CONTAINER);
        roundRectRenderer.addRoundRect(handleX, handleY, handleWidth, handleHeight, 2.0f, DropdownTheme.PRIMARY);

        if (indicatorProgress > 0.01f) {
            String label = formatValue();
            float textScale = 0.62f;
            float bubbleWidth = textRenderer.getWidth(label, textScale) + 16.0f;
            float bubbleHeight = 18.0f;
            float bubbleX = handleX + handleWidth / 2.0f - bubbleWidth / 2.0f;
            float bubbleY = bounds.y() - 22.0f;
            int bubbleAlpha = (int) (255 * indicatorProgress);
            roundRectRenderer.addRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 9.0f, DropdownTheme.withAlpha(DropdownTheme.INVERSE_SURFACE, bubbleAlpha));
            float textWidth = textRenderer.getWidth(label, textScale);
            float textHeight = textRenderer.getHeight(textScale);
            float textX = bubbleX + (bubbleWidth - textWidth) / 2.0f;
            float textY = bubbleY + (bubbleHeight - textHeight) / 2.0f - 1.0f;
            textRenderer.addText(label, textX, textY, textScale, DropdownTheme.withAlpha(DropdownTheme.INVERSE_ON_SURFACE, bubbleAlpha));
        }

        Color fieldBase = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.SURFACE_CONTAINER_LOW;
        Color fieldHover = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGHEST : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color fieldColor = focused
                ? DropdownTheme.INVERSE_SURFACE
                : DropdownTheme.lerp(fieldBase, fieldHover, animatedHover * 0.85f);
        Color fieldTextColor = focused ? DropdownTheme.INVERSE_ON_SURFACE : DropdownTheme.TEXT_PRIMARY;
        roundRectRenderer.addRoundRect(fieldBounds.x(), fieldBounds.y(), fieldBounds.width(), fieldBounds.height(), 7.0f, fieldColor);
        if (focused) {
            rectRenderer.addRect(fieldBounds.x() + 5.0f, fieldBounds.bottom() - 2.0f, fieldBounds.width() - 10.0f, 1.5f, DropdownTheme.PRIMARY);
        }
        String display = focused ? getDisplayBuffer() : formatValue();
        float displayScale = 0.60f;
        float displayWidth = textRenderer.getWidth(display, displayScale);
        float displayHeight = textRenderer.getHeight(displayScale);
        float displayX = fieldBounds.x() + (fieldBounds.width() - displayWidth) / 2.0f;
        float displayY = fieldBounds.y() + (fieldBounds.height() - displayHeight) / 2.0f - 1.0f;
        textRenderer.addText(display, displayX, displayY, displayScale, fieldTextColor);
        if (focused) {
            float caretX = displayX + textRenderer.getWidth(display.substring(0, Math.min(cursorIndex, display.length())), displayScale);
            rectRenderer.addRect(caretX, fieldBounds.y() + 4.0f, 1.0f, fieldBounds.height() - 8.0f, DropdownTheme.INVERSE_ON_SURFACE);
        }
    }

    public DropdownLayout.Rect getTrackBounds(DropdownLayout.Rect bounds) {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 116.0f, bounds.y() + 12.0f, 72.0f, 6.0f);
    }

    public DropdownLayout.Rect getFieldBounds(DropdownLayout.Rect bounds) {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 40.0f, bounds.y() + 4.0f, 40.0f, 18.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        DropdownLayout.Rect fieldBounds = getFieldBounds(bounds);
        if (event.button() == 0 && fieldBounds.contains(event.x(), event.y())) {
            dragging = false;
            focused = true;
            inputBuffer = formatPlainValue();
            cursorIndex = getCursorIndex(event.x(), fieldBounds);
            return true;
        }
        if (event.button() != 0 || !getInteractiveBounds(bounds).contains(event.x(), event.y())) {
            return false;
        }
        focused = false;
        dragging = true;
        updateFromMouse(bounds, event.x());
        return true;
    }

    @Override
    public boolean mouseReleased(DropdownLayout.Rect bounds, MouseButtonEvent event) {
        dragging = false;
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!focused) {
            return false;
        }
        return switch (event.key()) {
            case 257, 335 -> {
                commitInput();
                focused = false;
                yield true;
            }
            case 256 -> {
                focused = false;
                inputBuffer = null;
                yield true;
            }
            case 259 -> {
                if (inputBuffer != null && cursorIndex > 0) {
                    inputBuffer = inputBuffer.substring(0, cursorIndex - 1) + inputBuffer.substring(cursorIndex);
                    cursorIndex--;
                }
                yield true;
            }
            case 261 -> {
                if (inputBuffer != null && cursorIndex < inputBuffer.length()) {
                    inputBuffer = inputBuffer.substring(0, cursorIndex) + inputBuffer.substring(cursorIndex + 1);
                }
                yield true;
            }
            case 263 -> {
                cursorIndex = Math.max(0, cursorIndex - 1);
                yield true;
            }
            case 262 -> {
                cursorIndex = Math.min(getDisplayBuffer().length(), cursorIndex + 1);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) {
            return false;
        }
        String value = event.codepointAsString();
        if (!value.matches("[0-9.-]")) {
            return false;
        }
        String current = getDisplayBuffer();
        inputBuffer = current.substring(0, cursorIndex) + value + current.substring(cursorIndex);
        cursorIndex++;
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused && this.focused) {
            commitInput();
            inputBuffer = null;
        }
        this.focused = focused;
        if (focused && inputBuffer == null) {
            inputBuffer = formatPlainValue();
            cursorIndex = inputBuffer.length();
        }
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    public void updateFromMouse(DropdownLayout.Rect bounds, double mouseX) {
        DropdownLayout.Rect trackBounds = getTrackBounds(bounds);
        double progress = (mouseX - trackBounds.x()) / trackBounds.width();
        progress = Math.max(0.0, Math.min(1.0, progress));
        double rawValue = setting.getMin() + (setting.getMax() - setting.getMin()) * progress;
        double step = setting.getStep() <= 0.0 ? 0.01 : setting.getStep();
        double snapped = setting.getMin() + Math.round((rawValue - setting.getMin()) / step) * step;
        setting.setValue(snapped);
    }

    public boolean isDragging() {
        return dragging;
    }

    private DropdownLayout.Rect getInteractiveBounds(DropdownLayout.Rect bounds) {
        DropdownLayout.Rect track = getTrackBounds(bounds);
        return new DropdownLayout.Rect(track.x(), track.y() - 6.0f, track.width(), track.height() + 12.0f);
    }

    private float getProgress() {
        if (setting.getMax() <= setting.getMin()) {
            return 0.0f;
        }
        return (float) ((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
    }

    private boolean shouldDrawSteps() {
        double step = setting.getStep() <= 0.0 ? 0.0 : setting.getStep();
        double range = setting.getMax() - setting.getMin();
        return range > 0.0 && step / range > 0.08;
    }

    private void drawSteps(RectRenderer rectRenderer, DropdownLayout.Rect trackBounds, float progress) {
        double step = setting.getStep() <= 0.0 ? 0.0 : setting.getStep();
        double range = setting.getMax() - setting.getMin();
        int steps = Math.max(1, (int) Math.floor(range / step));
        float dotSize = 1.5f;
        for (int i = 0; i <= steps; i++) {
            float stepProgress = i / (float) steps;
            if (Math.abs(stepProgress - progress) < (1.0f / steps) * 0.5f) {
                continue;
            }
            float x = trackBounds.x() + trackBounds.width() * stepProgress - dotSize / 2.0f;
            x = Mth.clamp(x, trackBounds.x(), trackBounds.right() - dotSize);
            float y = trackBounds.centerY() - dotSize / 2.0f;
            rectRenderer.addRect(x, y, dotSize, dotSize, stepProgress <= progress ? DropdownTheme.ON_PRIMARY : DropdownTheme.ON_SECONDARY_CONTAINER);
        }
    }

    private String formatValue() {
        String label = Math.abs(setting.getValue() - Math.round(setting.getValue())) < 0.0001 ? Integer.toString((int) Math.round(setting.getValue())) : String.format("%.2f", setting.getValue());
        if (setting.isPercentageMode()) {
            label += "%";
        }
        return label;
    }

    private String formatPlainValue() {
        return Math.abs(setting.getValue() - Math.round(setting.getValue())) < 0.0001 ? Integer.toString((int) Math.round(setting.getValue())) : String.format("%.2f", setting.getValue());
    }

    private String getDisplayBuffer() {
        return inputBuffer == null ? formatPlainValue() : inputBuffer;
    }

    private void commitInput() {
        if (inputBuffer == null || inputBuffer.isBlank() || "-".equals(inputBuffer) || ".".equals(inputBuffer)) {
            inputBuffer = formatPlainValue();
            cursorIndex = inputBuffer.length();
            return;
        }
        try {
            setting.setValue(Double.parseDouble(inputBuffer));
        } catch (NumberFormatException ignored) {
        }
        inputBuffer = formatPlainValue();
        cursorIndex = inputBuffer.length();
    }

    private int getCursorIndex(double mouseX, DropdownLayout.Rect fieldBounds) {
        String text = getDisplayBuffer();
        float scale = 0.60f;
        float textWidth = measureTextRenderer.getWidth(text, scale);
        float textStart = fieldBounds.x() + (fieldBounds.width() - textWidth) / 2.0f;
        for (int i = 0; i <= text.length(); i++) {
            float width = measureTextRenderer.getWidth(text.substring(0, i), scale);
            if (mouseX <= textStart + width) {
                return i;
            }
        }
        return text.length();
    }

}
