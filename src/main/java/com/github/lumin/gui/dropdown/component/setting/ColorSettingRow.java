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
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);
        float swatchX = bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 12.0f;
        roundRectRenderer.addRoundRect(swatchX, bounds.y() + 7.0f, 12.0f, 12.0f, 5.0f, setting.getValue());
        rectRenderer.addRect(swatchX, bounds.y() + 20.0f, 12.0f, 1.0f, DropdownTheme.OUTLINE_SOFT);
    }
}
