package com.github.lumin.gui.dropdown.component;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.adapter.ModuleViewModel;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.*;

public class ModuleRow {

    public static final float HEIGHT = 34.0f;

    private final ModuleViewModel module;
    private final DropdownLayout.Rect bounds;

    public ModuleRow(ModuleViewModel module, DropdownLayout.Rect bounds) {
        this.module = module;
        this.bounds = bounds;
    }

    public ModuleViewModel getModule() {
        return module;
    }

    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    public DropdownLayout.Rect getToggleBounds() {
        return new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 30.0f, bounds.y() + 8.0f, 30.0f, 16.0f);
    }

    public void render(RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, float hoverProgress, float selectedProgress, float toggleProgress, float toggleHoverProgress) {
        float titleScale = 0.70f;
        float subScale = 0.60f;
        float keyScale = 0.6f;
        float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
        float subHeight = textRenderer.getHeight(subScale);
        float lineGap = 3.0f;
        float totalTextHeight = titleHeight + lineGap + subHeight;
        float titleY = bounds.y() + (bounds.height() - totalTextHeight) / 2.0f - 1.0f;
        float subY = titleY + titleHeight + lineGap - 1.0f;
        float keyY = bounds.y() + (bounds.height() - textRenderer.getHeight(keyScale)) / 2.0f - 1.0f;
        Color hoverBackground = DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress);
        Color background = DropdownTheme.lerp(hoverBackground, DropdownTheme.PRIMARY_CONTAINER, selectedProgress);
        Color titleColor = DropdownTheme.lerp(DropdownTheme.TEXT_PRIMARY, DropdownTheme.ON_PRIMARY_CONTAINER, selectedProgress);
        Color subColor = DropdownTheme.lerp(DropdownTheme.TEXT_SECONDARY, DropdownTheme.withAlpha(DropdownTheme.ON_PRIMARY_CONTAINER, 180), selectedProgress);
        Color keyColor = DropdownTheme.isLightTheme() ? DropdownTheme.TEXT_SECONDARY : DropdownTheme.TEXT_MUTED;
        String keybindText = formatKeybind(module.module().getKeyBind());
        float keyWidth = textRenderer.getWidth(keybindText, keyScale);
        float keyX = getToggleBounds().x() - 8.0f - keyWidth;

        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, background);

        textRenderer.addText(module.displayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET + 4.0f, titleY, titleScale, titleColor, StaticFontLoader.DUCKSANS);
        textRenderer.addText(module.category().getName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET + 4.0f, subY, subScale, subColor);
        drawSwitch(roundRectRenderer, getToggleBounds(), toggleProgress, toggleHoverProgress);
        textRenderer.addText(keybindText, keyX, keyY, keyScale, keyColor);
    }

    private void drawSwitch(RoundRectRenderer roundRectRenderer, DropdownLayout.Rect rect, float toggleProgress, float toggleHoverProgress) {
        Color trackBase = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color knobBase = DropdownTheme.isLightTheme() ? DropdownTheme.TEXT_SECONDARY : DropdownTheme.OUTLINE;
        Color track = DropdownTheme.lerp(trackBase, DropdownTheme.PRIMARY, toggleProgress);
        Color knob = DropdownTheme.lerp(knobBase, DropdownTheme.ON_PRIMARY_CONTAINER, toggleProgress);
        float knobSize = 8.0f + 3.0f * toggleProgress;
        float knobTravel = rect.width() - 10.0f - knobSize;
        float knobX = rect.x() + 5.0f + knobTravel * toggleProgress;
        float knobY = rect.centerY() - knobSize / 2.0f;
        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.height() / 2.0f, track);
        if (toggleHoverProgress > 0.01f) {
            float haloSize = 16.0f;
            float haloX = knobX + knobSize / 2.0f - haloSize / 2.0f;
            float haloY = rect.centerY() - haloSize / 2.0f;
            roundRectRenderer.addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f, DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (18 * toggleHoverProgress)));
        }
        roundRectRenderer.addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, knob);
    }

    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return "NONE";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString().toUpperCase();
    }

}
