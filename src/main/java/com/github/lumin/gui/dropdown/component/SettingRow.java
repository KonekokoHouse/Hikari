package com.github.lumin.gui.dropdown.component;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.settings.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class SettingRow<T extends Setting<?>> {

    protected final T setting;

    protected SettingRow(T setting) {
        this.setting = setting;
    }

    public T getSetting() {
        return setting;
    }

    public float getHeight() {
        return 28.0f;
    }

    public void render(GuiGraphics guiGraphics, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, TextRenderer textRenderer, DropdownLayout.Rect bounds, float hoverProgress, int mouseX, int mouseY, float partialTick) {
    }

    public boolean mouseClicked(DropdownLayout.Rect bounds, MouseButtonEvent event, boolean isDoubleClick) {
        return false;
    }

    public boolean mouseReleased(DropdownLayout.Rect bounds, MouseButtonEvent event) {
        return false;
    }

    public boolean mouseScrolled(DropdownLayout.Rect bounds, double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

}
