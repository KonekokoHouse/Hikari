package com.github.lumin.gui.dropdown.component.setting;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.settings.impl.StringSetting;
import net.minecraft.client.gui.GuiGraphics;

public class StringSettingRow extends SettingRow<StringSetting> {

    public StringSettingRow(StringSetting setting) {
        super(setting);
    }

    @Override
    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER, DropdownTheme.SURFACE_CONTAINER_HIGH, hoverProgress));
        textRenderer.addText(setting.getDisplayName(), bounds.x() + DropdownTheme.ROW_CONTENT_INSET, bounds.y() + 7.0f, 0.68f, DropdownTheme.TEXT_PRIMARY);
        String value = setting.getValue();
        String shown = value == null ? "" : value;
        if (shown.length() > 22) {
            shown = shown.substring(0, 19) + "...";
        }
        textRenderer.addText(shown, bounds.right() - DropdownTheme.ROW_TRAILING_INSET - 92.0f, bounds.y() + 7.0f, 0.60f, DropdownTheme.TEXT_SECONDARY);
    }
}
