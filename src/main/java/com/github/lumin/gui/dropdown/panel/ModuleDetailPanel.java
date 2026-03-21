package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.adapter.SettingViewFactory;
import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.gui.dropdown.component.setting.DoubleSettingRow;
import com.github.lumin.gui.dropdown.component.setting.EnumSettingRow;
import com.github.lumin.gui.dropdown.component.setting.IntSettingRow;
import com.github.lumin.gui.dropdown.popup.DropdownPopupHost;
import com.github.lumin.gui.dropdown.popup.EnumSelectPopup;
import com.github.lumin.gui.dropdown.util.DropdownScissor;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleDetailPanel {

    protected final DropdownState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final ShadowRenderer shadowRenderer;
    private final TextRenderer textRenderer;
    private final DropdownPopupHost popupHost;
    private final RoundRectRenderer contentRoundRectRenderer = new RoundRectRenderer();
    private final RectRenderer contentRectRenderer = new RectRenderer();
    private final ShadowRenderer contentShadowRenderer = new ShadowRenderer();
    private final TextRenderer contentTextRenderer = new TextRenderer();
    private DropdownLayout.Rect bounds;
    private int guiHeight;
    private DropdownLayout.Rect headerBounds;
    private final List<SettingEntry> settingEntries = new ArrayList<>();
    private final Map<Setting<?>, Animation> hoverAnimations = new HashMap<>();
    private final Map<Setting<?>, SettingRow<?>> rowCache = new HashMap<>();
    private SettingEntry draggingSliderEntry;

    public ModuleDetailPanel(DropdownState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, DropdownPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.shadowRenderer = shadowRenderer;
        this.textRenderer = textRenderer;
        this.popupHost = popupHost;
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = guiGraphics.guiHeight();
        settingEntries.clear();
        boolean popupConsumesHover = popupHost.getActivePopup() != null && popupHost.getActivePopup().getBounds().contains(mouseX, mouseY);
        int effectiveMouseX = popupConsumesHover ? Integer.MIN_VALUE : mouseX;
        int effectiveMouseY = popupConsumesHover ? Integer.MIN_VALUE : mouseY;

        Module module = state.getSelectedModule();
        String detailTitle = module == null ? "No Module" : module.getTranslatedName();
        textRenderer.addText(detailTitle, bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("Settings", bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 21.0f, 0.56f, DropdownTheme.TEXT_SECONDARY);

        if (module == null) {
            return;
        }

        headerBounds = new DropdownLayout.Rect(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - DropdownTheme.PANEL_VIEWPORT_INSET * 2.0f, 52.0f);
        roundRectRenderer.addRoundRect(headerBounds.x(), headerBounds.y(), headerBounds.width(), headerBounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.SURFACE_CONTAINER);
        textRenderer.addText(module.getTranslatedName(), headerBounds.x() + DropdownTheme.PANEL_TITLE_INSET, headerBounds.y() + 8.0f, 0.72f, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText(module.category.getName(), headerBounds.x() + DropdownTheme.PANEL_TITLE_INSET, headerBounds.y() + 18.0f, 0.56f, DropdownTheme.TEXT_SECONDARY);
        textRenderer.addText(module.getDescription(), headerBounds.x() + DropdownTheme.PANEL_TITLE_INSET, headerBounds.y() + 28.0f, 0.56f, DropdownTheme.TEXT_MUTED);
        drawSwitch(new DropdownLayout.Rect(headerBounds.right() - DropdownTheme.ROW_TRAILING_INSET - 24.0f, headerBounds.y() + 8.0f, 24.0f, 14.0f), module.isEnabled());
        textRenderer.addText(module.getBindMode().name(), headerBounds.right() - DropdownTheme.ROW_TRAILING_INSET - 34.0f, headerBounds.y() + 28.0f, 0.56f, DropdownTheme.TEXT_SECONDARY);

        DropdownLayout.Rect viewport = getViewport();
        List<Setting<?>> settings = module.getSettings().stream().filter(Setting::isAvailable).toList();
        rowCache.keySet().removeIf(setting -> !settings.contains(setting));
        float contentHeight = settings.size() * (28.0f + DropdownTheme.ROW_GAP);
        state.setMaxDetailScroll(contentHeight - viewport.height());

        DropdownScissor.apply(viewport, contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer, guiHeight);
        float y = viewport.y() - state.getDetailScroll();
        for (Setting<?> setting : settings) {
            SettingRow<?> row = rowCache.computeIfAbsent(setting, SettingViewFactory::create);
            if (row == null) {
                continue;
            }
            DropdownLayout.Rect rowBounds = new DropdownLayout.Rect(viewport.x(), y, viewport.width(), row.getHeight());
            settingEntries.add(new SettingEntry(row, rowBounds));
            Animation hoverAnimation = hoverAnimations.computeIfAbsent(setting, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
            hoverAnimation.run(rowBounds.contains(effectiveMouseX, effectiveMouseY) ? 1.0f : 0.0f);
            row.render(guiGraphics, contentRoundRectRenderer, contentRectRenderer, contentTextRenderer, rowBounds, hoverAnimation.getValue(), effectiveMouseX, effectiveMouseY, partialTick);
            y += row.getHeight() + DropdownTheme.ROW_GAP;
        }
        contentShadowRenderer.drawAndClear();
        contentRoundRectRenderer.drawAndClear();
        contentRectRenderer.drawAndClear();
        contentTextRenderer.drawAndClear();
        DropdownScissor.clear(contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        Module module = state.getSelectedModule();
        if (module == null || headerBounds == null) {
            return false;
        }

        DropdownLayout.Rect toggle = new DropdownLayout.Rect(headerBounds.right() - DropdownTheme.ROW_TRAILING_INSET - 24.0f, headerBounds.y() + 8.0f, 24.0f, 14.0f);
        if (toggle.contains(event.x(), event.y())) {
            module.toggle();
            return true;
        }

        for (SettingEntry entry : settingEntries) {
            if (entry.row instanceof IntSettingRow intRow && intRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                draggingSliderEntry = entry;
                intRow.updateFromMouse(entry.bounds, event.x());
                return true;
            }
            if (entry.row instanceof DoubleSettingRow doubleRow && doubleRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                draggingSliderEntry = entry;
                doubleRow.updateFromMouse(entry.bounds, event.x());
                return true;
            }
            if (entry.row instanceof EnumSettingRow enumRow && entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                popupHost.open(createEnumPopup(enumRow, entry.bounds));
                return true;
            }
            if (entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSliderEntry != null) {
            draggingSliderEntry.row.mouseReleased(draggingSliderEntry.bounds, event);
        }
        draggingSliderEntry = null;
        return false;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (draggingSliderEntry == null || event.button() != 0) {
            return false;
        }
        double currentMouseX = event.x();
        if (draggingSliderEntry.row instanceof IntSettingRow intRow) {
            intRow.updateFromMouse(draggingSliderEntry.bounds, currentMouseX);
            return true;
        }
        if (draggingSliderEntry.row instanceof DoubleSettingRow doubleRow) {
            doubleRow.updateFromMouse(draggingSliderEntry.bounds, currentMouseX);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DropdownLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollDetail(-scrollY * 20.0f);
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    private DropdownLayout.Rect getViewport() {
        if (bounds == null) {
            return new DropdownLayout.Rect(0, 0, 0, 0);
        }
        if (headerBounds == null) {
            return new DropdownLayout.Rect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        }
        return new DropdownLayout.Rect(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, headerBounds.bottom() + 6.0f, bounds.width() - DropdownTheme.PANEL_VIEWPORT_INSET * 2.0f, bounds.bottom() - headerBounds.bottom() - 10.0f);
    }

    private void drawSwitch(DropdownLayout.Rect rect, boolean enabled) {
        Color track = enabled ? DropdownTheme.PRIMARY : DropdownTheme.SURFACE_CONTAINER_HIGHEST;
        Color knob = enabled ? DropdownTheme.ON_PRIMARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;
        roundRectRenderer.addRoundRect(rect.x(), rect.y(), rect.width(), rect.height(), DropdownTheme.CHIP_RADIUS, track);
        float knobSize = rect.height() - 6.0f;
        float knobX = enabled ? rect.right() - knobSize - 3.0f : rect.x() + 3.0f;
        roundRectRenderer.addRoundRect(knobX, rect.y() + 3.0f, knobSize, knobSize, DropdownTheme.CHIP_RADIUS, knob);
    }

    private record SettingEntry(SettingRow<?> row, DropdownLayout.Rect bounds) {
    }

    private EnumSelectPopup createEnumPopup(EnumSettingRow enumRow, DropdownLayout.Rect rowBounds) {
        DropdownLayout.Rect chipBounds = enumRow.getChipBounds(textRenderer, rowBounds);
        int optionCount = enumRow.getSetting().getModes().length;
        float popupHeight = optionCount * 24.0f + 12.0f;
        float popupWidth = Math.max(108.0f, chipBounds.width() + 24.0f);
        float popupX = Math.max(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, chipBounds.right() - popupWidth);
        float popupY = chipBounds.bottom() + 4.0f;
        float maxBottom = bounds.bottom() - DropdownTheme.PANEL_VIEWPORT_INSET;
        if (popupY + popupHeight > maxBottom) {
            popupY = chipBounds.y() - popupHeight - 4.0f;
        }
        return new EnumSelectPopup(new DropdownLayout.Rect(popupX, popupY, popupWidth, popupHeight), chipBounds, enumRow.getSetting());
    }
}
