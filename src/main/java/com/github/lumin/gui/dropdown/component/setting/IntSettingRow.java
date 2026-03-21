package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.IntSetting;
import net.minecraft.client.gui.GuiGraphics;

public class IntSettingRow extends SettingRow<IntSetting> {

    public IntSettingRow(IntSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);
        double normalized = setting.getMax() <= setting.getMin() ? 0.0 : (setting.getValue() - setting.getMin()) / (double) (setting.getMax() - setting.getMin());
        float trackX = bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 96.0f;
        float trackY = bounds.y() + 18.0f;
        float trackWidth = 66.0f;
        rectRenderer.addRect(trackX, trackY, trackWidth, 2.0f, DropdownTheme.OUTLINE_SOFT);
        rectRenderer.addRect(trackX, trackY, (float) (trackWidth * Math.max(0.0, Math.min(1.0, normalized))), 2.0f, DropdownTheme.PRIMARY);
        String label = setting.isPercentageMode() ? setting.getValue() + "%" : Integer.toString(setting.getValue());
        textRenderer.addText(label, bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 22.0f, bounds.y() + 7.0f, 0.60f, DropdownTheme.TEXT_SECONDARY);
    }
}
