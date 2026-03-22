package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.shaders.BlurShader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.awt.*;

public class ColorPickerPopup implements DropdownPopupHost.Popup {

    private enum Channel {
        RED("R", new Color(239, 83, 80)),
        GREEN("G", new Color(102, 187, 106)),
        BLUE("B", new Color(66, 165, 245)),
        ALPHA("A", DropdownTheme.PRIMARY);

        private final String shortLabel;
        private final Color accent;

        Channel(String shortLabel, Color accent) {
            this.shortLabel = shortLabel;
            this.accent = accent;
        }
    }

    private final DropdownLayout.Rect bounds;
    private final DropdownLayout.Rect anchorBounds;
    private final ColorSetting setting;
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final RectRenderer rectRenderer = new RectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 160L);
    private final Animation[] indicatorAnimations = new Animation[]{
            new Animation(Easing.EASE_OUT_CUBIC, 120L),
            new Animation(Easing.EASE_OUT_CUBIC, 120L),
            new Animation(Easing.EASE_OUT_CUBIC, 120L),
            new Animation(Easing.EASE_OUT_CUBIC, 120L)
    };
    private Channel draggingChannel;
    private Channel focusedChannel;
    private String inputBuffer;
    private int cursorIndex;

    public ColorPickerPopup(DropdownLayout.Rect bounds, DropdownLayout.Rect anchorBounds, ColorSetting setting) {
        this.bounds = bounds;
        this.anchorBounds = anchorBounds;
        this.setting = setting;
        this.openAnimation.setStartValue(0.0f);
        for (Animation animation : indicatorAnimations) {
            animation.setStartValue(0.0f);
        }
    }

    @Override
    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        openAnimation.run(1.0f);
        float progress = openAnimation.getValue();
        float popupY = bounds.y() - (1.0f - progress) * 6.0f;
        int alpha = (int) (245 * progress);

        if (ClickGui.INSTANCE.shouldBlur()) {
            BlurShader.INSTANCE.drawBlur(bounds.x(), popupY, bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, 10.0f);
        }
        shadowRenderer.addShadow(bounds.x(), popupY, bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, 14.0f, DropdownTheme.withAlpha(DropdownTheme.SHADOW, (int) (120 * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), popupY, bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER_LOW, alpha));
        roundRectRenderer.addRoundRect(anchorBounds.x(), anchorBounds.y(), anchorBounds.width(), anchorBounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.withAlpha(DropdownTheme.SECONDARY_CONTAINER, (int) (120 * progress)));

        DropdownLayout.Rect previewBounds = getPreviewBounds(popupY);
        Color previewSurface = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER;
        roundRectRenderer.addRoundRect(previewBounds.x(), previewBounds.y(), previewBounds.width(), previewBounds.height(), 10.0f, DropdownTheme.withAlpha(previewSurface, alpha));
        DropdownLayout.Rect swatchBounds = getPreviewSwatchBounds(popupY);
        Color swatchSurface = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 8.0f, DropdownTheme.withAlpha(swatchSurface, alpha));
        if (setting.isAllowAlpha()) {
            drawCheckerboard(swatchBounds, alpha);
        }
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 8.0f, setting.getValue());
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 8.0f, DropdownTheme.withAlpha(DropdownTheme.OUTLINE_SOFT, (int) (72 * progress)));

        String hex = formatHex(setting.getValue());
        String meta = setting.isAllowAlpha() ? "RGBA" : "RGB";
        textRenderer.addText(hex, previewBounds.x() + 40.0f, previewBounds.y() + 8.0f, 0.64f, DropdownTheme.TEXT_PRIMARY);
        textRenderer.addText(meta, previewBounds.x() + 40.0f, previewBounds.y() + 19.0f, 0.52f, DropdownTheme.TEXT_SECONDARY);

        Channel[] channels = getChannels();
        for (int i = 0; i < channels.length; i++) {
            Channel channel = channels[i];
            DropdownLayout.Rect rowBounds = getChannelRowBounds(popupY, i);
            DropdownLayout.Rect trackBounds = getTrackBounds(rowBounds);
            DropdownLayout.Rect valueBounds = getValueBounds(rowBounds);
            boolean hovered = rowBounds.contains(mouseX, mouseY);
            float channelProgress = getChannelValue(channel) / 255.0f;
            float activeWidth = Math.max(3.0f, trackBounds.width() * channelProgress);
            float handleX = trackBounds.x() + trackBounds.width() * channelProgress - 2.0f;
            Animation indicatorAnimation = indicatorAnimations[i];
            indicatorAnimation.run((hovered || draggingChannel == channel) ? 1.0f : 0.0f);
            float indicatorProgress = indicatorAnimation.getValue();
            Color rowSurface = hovered
                    ? (DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGHEST : DropdownTheme.SURFACE_CONTAINER_HIGH)
                    : (DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.SURFACE_CONTAINER);
            textRenderer.addText(channel.shortLabel, rowBounds.x() + 8.0f, rowBounds.y() + 6.5f, 0.58f, DropdownTheme.withAlpha(DropdownTheme.isLightTheme() ? DropdownTheme.TEXT_MUTED : DropdownTheme.TEXT_SECONDARY, alpha));
            roundRectRenderer.addRoundRect(rowBounds.x(), rowBounds.y(), rowBounds.width(), rowBounds.height(), 8.0f, DropdownTheme.withAlpha(rowSurface, alpha));
            roundRectRenderer.addRoundRect(trackBounds.x(), trackBounds.y(), trackBounds.width(), trackBounds.height(), 2.5f, DropdownTheme.withAlpha(DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER_HIGHEST, alpha));
            roundRectRenderer.addRoundRect(trackBounds.x(), trackBounds.y(), activeWidth, trackBounds.height(), 2.5f, 0.0f, 0.0f, 2.5f, DropdownTheme.withAlpha(channel.accent, alpha));
            roundRectRenderer.addRoundRect(handleX, trackBounds.centerY() - 6.0f, 4.0f, 12.0f, 2.0f, DropdownTheme.withAlpha(DropdownTheme.ON_PRIMARY_CONTAINER, alpha));
            String valueText = focusedChannel == channel ? getDisplayBuffer() : Integer.toString(getChannelValue(channel));
            drawValueBox(valueBounds, valueText, focusedChannel == channel, alpha);
            if (indicatorProgress > 0.01f) {
                drawValueIndicator(handleX + 2.0f, rowBounds.y() - 4.0f, Integer.toString(getChannelValue(channel)), indicatorProgress);
            }
        }

        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        rectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0 || !bounds.contains(event.x(), event.y())) {
            return false;
        }
        Channel[] channels = getChannels();
        for (int i = 0; i < channels.length; i++) {
            DropdownLayout.Rect rowBounds = getChannelRowBounds(bounds.y(), i);
            DropdownLayout.Rect trackBounds = getTrackBounds(rowBounds);
            DropdownLayout.Rect valueBounds = getValueBounds(rowBounds);
            if (valueBounds.contains(event.x(), event.y())) {
                draggingChannel = null;
                focusedChannel = channels[i];
                inputBuffer = Integer.toString(getChannelValue(focusedChannel));
                cursorIndex = getCursorIndex(event.x(), valueBounds, inputBuffer);
                return true;
            }
            DropdownLayout.Rect interactiveBounds = new DropdownLayout.Rect(trackBounds.x(), trackBounds.y() - 5.0f, trackBounds.width(), trackBounds.height() + 10.0f);
            if (interactiveBounds.contains(event.x(), event.y())) {
                draggingChannel = channels[i];
                focusedChannel = null;
                inputBuffer = null;
                updateChannelFromMouse(draggingChannel, event.x(), trackBounds);
                return true;
            }
        }
        focusedChannel = null;
        inputBuffer = null;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingChannel = null;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (draggingChannel == null || event.button() != 0) {
            return false;
        }
        int index = getChannelIndex(draggingChannel);
        DropdownLayout.Rect trackBounds = getTrackBounds(getChannelRowBounds(bounds.y(), index));
        updateChannelFromMouse(draggingChannel, event.x(), trackBounds);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focusedChannel == null) {
            return false;
        }
        return switch (event.key()) {
            case 257, 335 -> {
                commitInput();
                focusedChannel = null;
                inputBuffer = null;
                yield true;
            }
            case 256 -> {
                focusedChannel = null;
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
        if (focusedChannel == null || !event.isAllowedChatCharacter()) {
            return false;
        }
        String typed = event.codepointAsString();
        if (!typed.matches("[0-9]")) {
            return false;
        }
        String current = getDisplayBuffer();
        if (current.length() >= 3) {
            return true;
        }
        inputBuffer = current.substring(0, cursorIndex) + typed + current.substring(cursorIndex);
        cursorIndex++;
        return true;
    }

    private DropdownLayout.Rect getPreviewBounds(float popupY) {
        return new DropdownLayout.Rect(bounds.x() + 8.0f, popupY + 8.0f, bounds.width() - 16.0f, 34.0f);
    }

    private DropdownLayout.Rect getPreviewSwatchBounds(float popupY) {
        return new DropdownLayout.Rect(bounds.x() + 14.0f, popupY + 13.0f, 24.0f, 24.0f);
    }

    private DropdownLayout.Rect getChannelRowBounds(float popupY, int index) {
        float rowY = popupY + 50.0f + index * 24.0f;
        return new DropdownLayout.Rect(bounds.x() + 8.0f, rowY, bounds.width() - 16.0f, 20.0f);
    }

    private DropdownLayout.Rect getTrackBounds(DropdownLayout.Rect rowBounds) {
        return new DropdownLayout.Rect(rowBounds.x() + 18.0f, rowBounds.y() + 8.0f, rowBounds.width() - 48.0f, 4.0f);
    }

    private DropdownLayout.Rect getValueBounds(DropdownLayout.Rect rowBounds) {
        return new DropdownLayout.Rect(rowBounds.right() - 24.0f, rowBounds.y() + 2.0f, 18.0f, 16.0f);
    }

    private Channel[] getChannels() {
        return setting.isAllowAlpha()
                ? new Channel[]{Channel.RED, Channel.GREEN, Channel.BLUE, Channel.ALPHA}
                : new Channel[]{Channel.RED, Channel.GREEN, Channel.BLUE};
    }

    private int getChannelIndex(Channel channel) {
        Channel[] channels = getChannels();
        for (int i = 0; i < channels.length; i++) {
            if (channels[i] == channel) {
                return i;
            }
        }
        return 0;
    }

    private int getChannelValue(Channel channel) {
        return switch (channel) {
            case RED -> setting.getValue().getRed();
            case GREEN -> setting.getValue().getGreen();
            case BLUE -> setting.getValue().getBlue();
            case ALPHA -> setting.getValue().getAlpha();
        };
    }

    private void updateChannelFromMouse(Channel channel, double mouseX, DropdownLayout.Rect trackBounds) {
        double progress = (mouseX - trackBounds.x()) / trackBounds.width();
        progress = Mth.clamp(progress, 0.0, 1.0);
        int value = Mth.clamp((int) Math.round(progress * 255.0), 0, 255);
        Color current = setting.getValue();
        Color updated = switch (channel) {
            case RED -> new Color(value, current.getGreen(), current.getBlue(), current.getAlpha());
            case GREEN -> new Color(current.getRed(), value, current.getBlue(), current.getAlpha());
            case BLUE -> new Color(current.getRed(), current.getGreen(), value, current.getAlpha());
            case ALPHA -> new Color(current.getRed(), current.getGreen(), current.getBlue(), value);
        };
        setting.setValue(updated);
    }

    private String formatHex(Color color) {
        if (setting.isAllowAlpha()) {
            return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void drawValueIndicator(float anchorX, float rowTop, String valueText, float progress) {
        float textScale = 0.54f;
        float bubbleWidth = textRenderer.getWidth(valueText, textScale) + 12.0f;
        float bubbleHeight = 16.0f;
        float bubbleX = anchorX - bubbleWidth / 2.0f;
        float bubbleY = rowTop - 14.0f - (1.0f - progress) * 4.0f;
        int bubbleAlpha = (int) (255 * progress);
        roundRectRenderer.addRoundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 8.0f, DropdownTheme.withAlpha(DropdownTheme.INVERSE_SURFACE, bubbleAlpha));
        float textY = bubbleY + (bubbleHeight - textRenderer.getHeight(textScale)) / 2.0f - 1.0f;
        textRenderer.addText(valueText, bubbleX + (bubbleWidth - textRenderer.getWidth(valueText, textScale)) / 2.0f, textY, textScale, DropdownTheme.withAlpha(DropdownTheme.INVERSE_ON_SURFACE, bubbleAlpha));
    }

    private void drawValueBox(DropdownLayout.Rect bounds, String valueText, boolean focused, int alpha) {
        Color boxBase = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.SURFACE_CONTAINER_HIGH;
        Color boxHover = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGHEST : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color boxColor = focused ? DropdownTheme.INVERSE_SURFACE : DropdownTheme.withAlpha(DropdownTheme.lerp(boxBase, boxHover, 0.85f), alpha);
        Color textColor = focused ? DropdownTheme.INVERSE_ON_SURFACE : DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, alpha);
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 6.0f, boxColor);
        float textScale = 0.52f;
        float textWidth = textRenderer.getWidth(valueText, textScale);
        float textHeight = textRenderer.getHeight(textScale);
        float textX = bounds.x() + (bounds.width() - textWidth) / 2.0f;
        float textY = bounds.y() + (bounds.height() - textHeight) / 2.0f - 1.0f;
        textRenderer.addText(valueText, textX, textY, textScale, textColor);
        if (focused) {
            rectRenderer.addRect(bounds.x() + 3.0f, bounds.bottom() - 2.0f, bounds.width() - 6.0f, 1.5f, DropdownTheme.withAlpha(DropdownTheme.PRIMARY, alpha));
            float caretX = textX + textRenderer.getWidth(valueText.substring(0, Math.min(cursorIndex, valueText.length())), textScale);
            rectRenderer.addRect(caretX, bounds.y() + 3.0f, 1.0f, bounds.height() - 6.0f, DropdownTheme.INVERSE_ON_SURFACE);
        }
    }

    private void drawCheckerboard(DropdownLayout.Rect bounds, int alpha) {
        float inset = 2.0f;
        float cell = 4.0f;
        Color dark = DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, alpha);
        Color light = DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER_HIGH, alpha);
        for (float y = bounds.y() + inset; y < bounds.bottom() - inset; y += cell) {
            for (float x = bounds.x() + inset; x < bounds.right() - inset; x += cell) {
                int ix = (int) Math.floor((x - bounds.x()) / cell);
                int iy = (int) Math.floor((y - bounds.y()) / cell);
                Color color = ((ix + iy) & 1) == 0 ? light : dark;
                float width = Math.min(cell, bounds.right() - inset - x);
                float height = Math.min(cell, bounds.bottom() - inset - y);
                roundRectRenderer.addRoundRect(x, y, width, height, 0.0f, color);
            }
        }
    }

    private void commitInput() {
        if (focusedChannel == null || inputBuffer == null || inputBuffer.isBlank()) {
            return;
        }
        int value;
        try {
            value = Mth.clamp(Integer.parseInt(inputBuffer), 0, 255);
        } catch (NumberFormatException ignored) {
            value = getChannelValue(focusedChannel);
        }
        Color current = setting.getValue();
        Color updated = switch (focusedChannel) {
            case RED -> new Color(value, current.getGreen(), current.getBlue(), current.getAlpha());
            case GREEN -> new Color(current.getRed(), value, current.getBlue(), current.getAlpha());
            case BLUE -> new Color(current.getRed(), current.getGreen(), value, current.getAlpha());
            case ALPHA -> new Color(current.getRed(), current.getGreen(), current.getBlue(), value);
        };
        setting.setValue(updated);
        inputBuffer = Integer.toString(value);
        cursorIndex = inputBuffer.length();
    }

    private String getDisplayBuffer() {
        return inputBuffer == null ? Integer.toString(getChannelValue(focusedChannel)) : inputBuffer;
    }

    private int getCursorIndex(double mouseX, DropdownLayout.Rect fieldBounds, String text) {
        float scale = 0.52f;
        float textWidth = textRenderer.getWidth(text, scale);
        float textStart = fieldBounds.x() + (fieldBounds.width() - textWidth) / 2.0f;
        for (int i = 0; i <= text.length(); i++) {
            float width = textRenderer.getWidth(text.substring(0, i), scale);
            if (mouseX <= textStart + width) {
                return i;
            }
        }
        return text.length();
    }

}
