package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.ColorSetting;
import net.minecraft.client.gui.GuiGraphics;

public class ColorSettingRow extends SettingRow<ColorSetting> {

    public ColorSettingRow(ColorSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        float labelScale = 0.68f;
        float labelY = bounds.y() + (bounds.height() - textRenderer.getHeight(labelScale)) / 2.0f - 1.0f;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, labelY, labelScale, DropdownTheme.TEXT_PRIMARY);
        DropdownLayout.Rect swatchBounds = getSwatchBounds(bounds);
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 5.0f, DropdownTheme.SURFACE_CONTAINER_HIGHEST);
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 5.0f, setting.getValue());
        roundRectRenderer.addRoundRect(swatchBounds.x(), swatchBounds.y(), swatchBounds.width(), swatchBounds.height(), 5.0f, DropdownTheme.withAlpha(DropdownTheme.OUTLINE_SOFT, 58));
    }

    public DropdownLayout.Rect getSwatchBounds(DropdownLayout.Rect bounds) {
        float swatchX = bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 12.0f;
        float swatchY = bounds.y() + (bounds.height() - 12.0f) / 2.0f;
        return new DropdownLayout.Rect(swatchX, swatchY, 12.0f, 12.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, net.minecraft.client.input.MouseButtonEvent event, boolean isDoubleClick) {
        return event.button() == 0 && bounds.contains(event.x(), event.y());
    }

}
