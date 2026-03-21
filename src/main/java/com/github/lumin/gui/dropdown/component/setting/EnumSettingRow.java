package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public class EnumSettingRow extends SettingRow<EnumSetting<?>> {

    public EnumSettingRow(EnumSetting<?> setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);
        DropdownLayout.Rect chipBounds = getChipBounds(textRenderer, bounds);
        float chipX = chipBounds.x();
        float chipWidth = chipBounds.width();
        roundRectRenderer.addRoundRect(chipX, chipBounds.y(), chipWidth, chipBounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.SECONDARY_CONTAINER);
        textRenderer.addText(setting.getTranslatedValue(), chipX + 8.0f, bounds.y() + 9.0f, 0.60f, DropdownTheme.ON_SECONDARY_CONTAINER);
        textRenderer.addText("V", chipBounds.right() - 10.0f, bounds.y() + 9.0f, 0.54f, DropdownTheme.ON_SECONDARY_CONTAINER);
    }

    public DropdownLayout.Rect getChipBounds(TextRenderer textRenderer, DropdownLayout.Rect bounds) {
        String value = setting.getTranslatedValue();
        float chipWidth = Math.min(96.0f, textRenderer.getWidth(value, 0.60f) + 26.0f);
        float chipX = bounds.right() - DropdownTheme.ROW_TRAILING_INSET - chipWidth;
        return new DropdownLayout.Rect(chipX, bounds.y() + 5.0f, chipWidth, 16.0f);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        return bounds.contains(event.x(), event.y()) && event.button() == 0;
    }

}
