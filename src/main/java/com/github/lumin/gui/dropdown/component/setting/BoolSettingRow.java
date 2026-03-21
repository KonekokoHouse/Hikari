package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.BoolSetting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class BoolSettingRow extends SettingRow<BoolSetting> {

    public BoolSettingRow(BoolSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);

        DropdownLayout.Rect rect = new DropdownLayout.Rect(bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 24.0f, bounds.y() + 7.0f, 24.0f, 14.0f);
        Color track = setting.getValue() ? DropdownTheme.PRIMARY : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color knob = setting.getValue() ? DropdownTheme.ON_PRIMARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;
        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), DropdownTheme.CHIP_RADIUS, track);
        float knobSize = rect.height() - 6.0f;
        float knobX = setting.getValue() ? rect.right() - knobSize - 3.0f : rect.x() + 3.0f;
        roundRectRenderer.addRoundRect(knobX, rect.y() + 3.0f, knobSize, knobSize, DropdownTheme.CHIP_RADIUS, knob);
    }

    @Override
    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        setting.setValue(!setting.getValue());
        return true;
    }
}
