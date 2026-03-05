package com.github.lumin.gui.clickgui.component.impl;

import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.Component;
import com.github.lumin.settings.impl.ModeSetting;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class ModeSettingComponent extends Component {
    private final ModeSetting setting;
    private final Animation selectedXAnimation = new Animation(Easing.EASE_OUT_QUAD, 150L);
    private boolean highlightInitialized;
    private float lastControlX;
    private float lastControlY;
    private float lastControlW;
    private float lastControlH;

    public ModeSettingComponent(ModeSetting setting) {
        this.setting = setting;
    }

    public ModeSetting getSetting() {
        return setting;
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (!setting.isAvailable()) return;

        boolean hovered = ColorSettingComponent.isMouseOutOfPicker(mouseX, mouseY) && MouseUtils.isHovering(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        Color bg = hovered ? new Color(255, 255, 255, (int) (18 * alpha)) : new Color(255, 255, 255, (int) (10 * alpha));
        set.bottomRoundRect().addRoundRect(getX(), getY(), getWidth(), getHeight(), 6.0f * scale, bg);

        String name = setting.getDisplayName();
        String[] modes = setting.getModes();
        if (modes == null) modes = new String[0];

        float textScale = 0.85f * scale;
        float textY = getY() + (getHeight() - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;
        float padding = 6.0f * scale;
        set.font().addText(name, getX() + padding, textY, textScale, new Color(255, 255, 255, (int) (255 * alpha)));

        if (modes.length == 0) {
            lastControlW = 0.0f;
            return;
        }

        float nameW = set.font().getWidth(name, textScale);
        float gap = 8.0f * scale;
        float controlH = Math.max(10.0f * scale, getHeight() - 4.0f * scale);
        float controlX = getX() + padding + nameW + gap;
        float controlY = getY() + (getHeight() - controlH) / 2.0f;
        float controlW = getX() + getWidth() - padding - controlX;

        if (controlW <= 8.0f * scale) {
            String value = String.valueOf(setting.getValue());
            float valueW = set.font().getWidth(value, textScale);
            set.font().addText(value, getX() + getWidth() - padding - valueW, textY, textScale, new Color(200, 200, 200, (int) (255 * alpha)));
            lastControlW = 0.0f;
            return;
        }

        lastControlX = controlX;
        lastControlY = controlY;
        lastControlW = controlW;
        lastControlH = controlH;

        float radius = Math.min(6.0f * scale, controlH / 2.0f);
        set.bottomRoundRect().addRoundRect(controlX, controlY, controlW, controlH, radius, new Color(0, 0, 0, (int) (70 * alpha)));

        int selectedIndex = setting.getModeIndex();
        selectedIndex = Math.max(0, Math.min(selectedIndex, modes.length - 1));

        float segW = controlW / modes.length;
        float segInnerPad = 6.0f * scale;

        float selectedX = controlX + segW * selectedIndex;
        if (!highlightInitialized) {
            selectedXAnimation.setStartValue(selectedX);
            selectedXAnimation.setValue(selectedX);
            highlightInitialized = true;
        }
        if (!selectedXAnimation.isFinished()) {
            selectedXAnimation.setStartValue(selectedX);
        }
        selectedXAnimation.run(selectedX);
        float ax = selectedXAnimation.getValue();
        Color selectedBg = new Color(255, 255, 255, (int) (26 * alpha));
        float selRadius = Math.min(6.0f * scale, controlH / 2.0f);

        if (selectedIndex == 0) {
            set.bottomRoundRect().addRoundRect(ax, controlY, segW, controlH, selRadius, 0.0f, 0.0f, selRadius, selectedBg);
        } else if (selectedIndex == modes.length - 1) {
            set.bottomRoundRect().addRoundRect(ax, controlY, segW, controlH, 0.0f, selRadius, selRadius, 0.0f, selectedBg);
        } else {
            set.bottomRoundRect().addRoundRect(ax, controlY, segW, controlH, 0.0f, 0.0f, 0.0f, 0.0f, selectedBg);
        }

        for (int i = 0; i < modes.length; i++) {
            float segX = controlX + segW * i;
            if (i > 0) {
                set.bottomRoundRect().addRoundRect(segX, controlY + 2.0f * scale, 1.0f * scale, controlH - 4.0f * scale, 0.0f, new Color(255, 255, 255, (int) (14 * alpha)));
            }
            String mode = modes[i] == null ? "" : modes[i];
            float maxTextW = Math.max(0.0f, segW - segInnerPad * 2.0f);
            String display = ellipsize(mode, set.font(), textScale, maxTextW);
            Color textColor = (i == selectedIndex) ? new Color(255, 255, 255, (int) (255 * alpha)) : new Color(200, 200, 200, (int) (255 * alpha));
            float textHeight = set.font().getHeight(textScale);
            float modeTextY = controlY + (controlH - textHeight) / 2.0f - 0.5f * scale;
            float modeW = set.font().getWidth(display, textScale);
            float modeX = segX + (segW - modeW) / 2.0f;
            set.font().addText(display, modeX, modeTextY, textScale, textColor);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (!setting.isAvailable()) return super.mouseClicked(event, focused);
        if (event.button() != 0) return super.mouseClicked(event, focused);
        String[] modes = setting.getModes();
        if (modes == null || modes.length == 0) return super.mouseClicked(event, focused);
        if (!MouseUtils.isHovering(lastControlX, lastControlY, lastControlW, lastControlH, event.x(), event.y())) {
            return super.mouseClicked(event, focused);
        }
        float segW = lastControlW / modes.length;
        if (segW <= 0.0f) return true;
        int index = (int) ((event.x() - lastControlX) / segW);
        index = Math.max(0, Math.min(index, modes.length - 1));
        setting.setMode(modes[index]);
        return true;
    }

    private static String ellipsize(String text, TextRenderer font, float scale, float maxWidth) {
        if (text == null) return "";
        if (maxWidth <= 0.0f) return "";
        if (font.getWidth(text, scale) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = font.getWidth(ellipsis, scale);
        if (ellipsisW > maxWidth) return "";
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            String candidate = text.substring(0, mid) + ellipsis;
            if (font.getWidth(candidate, scale) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo) + ellipsis;
    }
}