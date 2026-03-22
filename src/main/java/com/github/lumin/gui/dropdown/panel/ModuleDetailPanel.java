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
import com.github.lumin.gui.dropdown.component.setting.ColorSettingRow;
import com.github.lumin.gui.dropdown.component.setting.DoubleSettingRow;
import com.github.lumin.gui.dropdown.component.setting.EnumSettingRow;
import com.github.lumin.gui.dropdown.component.setting.IntSettingRow;
import com.github.lumin.gui.dropdown.popup.ColorPickerPopup;
import com.github.lumin.gui.dropdown.popup.DropdownPopupHost;
import com.github.lumin.gui.dropdown.popup.EnumSelectPopup;
import com.github.lumin.gui.dropdown.util.DropdownScissor;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
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
    private boolean contentPending;
    private final Animation bindModeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation bindModeHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation keybindFocusAnimation = new Animation(Easing.EASE_OUT_CUBIC, 150L);
    private final Animation moduleToggleAnimation = new Animation(Easing.DYNAMIC_ISLAND, 220L);
    private final Animation moduleToggleHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation moduleToggleHandleSizeAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);

    public ModuleDetailPanel(DropdownState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer, DropdownPopupHost popupHost) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.shadowRenderer = shadowRenderer;
        this.textRenderer = textRenderer;
        this.popupHost = popupHost;
        this.bindModeAnimation.setStartValue(0.0f);
        this.bindModeHoverAnimation.setStartValue(0.0f);
        this.keybindHoverAnimation.setStartValue(0.0f);
        this.keybindFocusAnimation.setStartValue(0.0f);
        this.moduleToggleAnimation.setStartValue(0.0f);
        this.moduleToggleHoverAnimation.setStartValue(0.0f);
        this.moduleToggleHandleSizeAnimation.setStartValue(8.0f);
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

        headerBounds = new DropdownLayout.Rect(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - DropdownTheme.PANEL_VIEWPORT_INSET * 2.0f, 62.0f);
        roundRectRenderer.addRoundRect(headerBounds.x(), headerBounds.y(), headerBounds.width(), headerBounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.SURFACE_CONTAINER);
        float titleScale = 0.72f;
        float metaScale = 0.56f;
        float titleHeight = textRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
        float metaHeight = textRenderer.getHeight(metaScale);
        float headerTextX = getHeaderContentInsetX();
        float titleY = headerBounds.y() + 8.0f;
        float categoryY = titleY + titleHeight + 3.0f;
        float descriptionY = categoryY + metaHeight + 2.5f;
        textRenderer.addText(module.getTranslatedName(), headerTextX, titleY, titleScale, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText(module.category.getName(), headerTextX, categoryY, metaScale, DropdownTheme.TEXT_SECONDARY);
        textRenderer.addText(module.getDescription(), headerTextX, descriptionY, metaScale, DropdownTheme.TEXT_MUTED);
        drawModuleToggleControl(module, mouseX, mouseY);
        drawKeybindControl(module, mouseX, mouseY);
        drawBindModeControl(module, mouseX, mouseY);

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
        contentPending = true;
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        Module module = state.getSelectedModule();
        if (module == null || headerBounds == null) {
            return false;
        }

        DropdownLayout.Rect moduleToggleBounds = getModuleToggleBounds();
        if (moduleToggleBounds.contains(event.x(), event.y())) {
            module.toggle();
            return true;
        }

        DropdownLayout.Rect keybindBounds = getKeybindBounds();
        if (keybindBounds.contains(event.x(), event.y())) {
            state.setListeningKeyBindModule(module);
            return true;
        } else if (state.getListeningKeyBindModule() == module) {
            state.setListeningKeyBindModule(null);
        }

        DropdownLayout.Rect bindModeBounds = getBindModeBounds();
        if (bindModeBounds.contains(event.x(), event.y())) {
            float midpoint = bindModeBounds.centerX();
            module.setBindMode(event.x() < midpoint ? Module.BindMode.Toggle : Module.BindMode.Hold);
            return true;
        }

        clearRowFocus();
        for (SettingEntry entry : settingEntries) {
            if (entry.row instanceof IntSettingRow intRow && intRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                if (intRow.isDragging()) {
                    draggingSliderEntry = entry;
                    intRow.updateFromMouse(entry.bounds, event.x());
                } else {
                    draggingSliderEntry = null;
                }
                return true;
            }
            if (entry.row instanceof DoubleSettingRow doubleRow && doubleRow.mouseClicked(entry.bounds, event, isDoubleClick)) {
                if (doubleRow.isDragging()) {
                    draggingSliderEntry = entry;
                    doubleRow.updateFromMouse(entry.bounds, event.x());
                } else {
                    draggingSliderEntry = null;
                }
                return true;
            }
            if (entry.row instanceof EnumSettingRow enumRow && entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                popupHost.open(createEnumPopup(enumRow, entry.bounds));
                return true;
            }
            if (entry.row instanceof ColorSettingRow colorRow && entry.row.mouseClicked(entry.bounds, event, isDoubleClick)) {
                popupHost.open(createColorPopup(colorRow, entry.bounds));
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
        Module module = state.getSelectedModule();
        if (module != null && state.getListeningKeyBindModule() == module) {
            if (event.key() == 256) {
                state.setListeningKeyBindModule(null);
                return true;
            }
            if (event.key() == 259 || event.key() == 261) {
                module.setKeyBind(-1);
                state.setListeningKeyBindModule(null);
                return true;
            }
            module.setKeyBind(event.key());
            state.setListeningKeyBindModule(null);
            return true;
        }
        for (SettingEntry entry : settingEntries) {
            if (entry.row.keyPressed(event)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        for (SettingEntry entry : settingEntries) {
            if (entry.row.charTyped(event)) {
                return true;
            }
        }
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

    private record SettingEntry(SettingRow<?> row, DropdownLayout.Rect bounds) {
    }

    private DropdownLayout.Rect getBindModeBounds() {
        return new DropdownLayout.Rect(getHeaderControlGroupX(), getHeaderSecondRowY(), getHeaderControlGroupWidth(), getHeaderControlHeight());
    }

    private DropdownLayout.Rect getModuleToggleBounds() {
        return new DropdownLayout.Rect(getHeaderControlGroupX(), getHeaderFirstRowY(), 36.0f, getHeaderControlHeight());
    }

    private DropdownLayout.Rect getKeybindBounds() {
        return new DropdownLayout.Rect(getHeaderControlGroupX() + 44.0f, getHeaderFirstRowY(), 80.0f, getHeaderControlHeight());
    }

    private float getHeaderControlGroupX() {
        return headerBounds.right() - getHeaderContentInset() - getHeaderControlGroupWidth();
    }

    private float getHeaderControlGroupWidth() {
        return 124.0f;
    }

    private float getHeaderContentInset() {
        return DropdownTheme.PANEL_TITLE_INSET + 2.0f;
    }

    private float getHeaderContentInsetX() {
        return headerBounds.x() + getHeaderContentInset();
    }

    private float getHeaderControlHeight() {
        return 18.0f;
    }

    private float getHeaderControlRadius() {
        return 9.0f;
    }

    private float getHeaderFirstRowY() {
        return headerBounds.y() + 10.0f;
    }

    private float getHeaderSecondRowY() {
        return getHeaderFirstRowY() + getHeaderControlHeight() + 9.0f;
    }

    private void drawHeaderControlSurface(DropdownLayout.Rect bounds, float hoverProgress, float focusProgress) {
        Color base = DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.SURFACE_CONTAINER_HIGH;
        roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), getHeaderControlRadius(), base);
        if (hoverProgress > 0.01f) {
            int hoverAlpha = DropdownTheme.isLightTheme() ? 8 : 12;
            roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), getHeaderControlRadius(), DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (hoverAlpha * hoverProgress)));
        }
        if (focusProgress > 0.01f) {
            int focusAlpha = DropdownTheme.isLightTheme() ? 12 : 10;
            roundRectRenderer.addRoundRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), getHeaderControlRadius(), DropdownTheme.withAlpha(DropdownTheme.PRIMARY, (int) (focusAlpha * focusProgress)));
        }
    }

    private void drawModuleToggleControl(Module module, int mouseX, int mouseY) {
        DropdownLayout.Rect toggleBounds = getModuleToggleBounds();
        moduleToggleAnimation.run(module.isEnabled() ? 1.0f : 0.0f);
        moduleToggleHoverAnimation.run(toggleBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        moduleToggleHandleSizeAnimation.run(module.isEnabled() ? 11.0f : 8.0f);

        float progress = moduleToggleAnimation.getValue();
        float hoverProgress = moduleToggleHoverAnimation.getValue();
        float handleSize = moduleToggleHandleSizeAnimation.getValue();
        Color track = DropdownTheme.lerp(DropdownTheme.isLightTheme() ? DropdownTheme.SURFACE_CONTAINER_HIGH : DropdownTheme.SURFACE_CONTAINER_HIGHEST, DropdownTheme.PRIMARY, progress);
        Color handle = DropdownTheme.lerp(DropdownTheme.isLightTheme() ? DropdownTheme.TEXT_SECONDARY : DropdownTheme.OUTLINE, DropdownTheme.ON_PRIMARY, progress);
        float handleTravel = toggleBounds.width() - 10.0f - handleSize;
        float handleX = toggleBounds.x() + 5.0f + handleTravel * progress;
        float handleY = toggleBounds.centerY() - handleSize / 2.0f;

        roundRectRenderer.addRoundRect(toggleBounds.x(), toggleBounds.y(), toggleBounds.width(), toggleBounds.height(), getHeaderControlRadius(), track);
        if (hoverProgress > 0.01f) {
            float haloSize = 16.0f;
            float haloX = handleX + handleSize / 2.0f - haloSize / 2.0f;
            float haloY = toggleBounds.centerY() - haloSize / 2.0f;
            roundRectRenderer.addRoundRect(haloX, haloY, haloSize, haloSize, haloSize / 2.0f, DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (18 * hoverProgress)));
        }
        roundRectRenderer.addRoundRect(handleX, handleY, handleSize, handleSize, handleSize / 2.0f, handle);
    }

    private void drawBindModeControl(Module module, int mouseX, int mouseY) {
        DropdownLayout.Rect bindModeBounds = getBindModeBounds();
        bindModeAnimation.run(module.getBindMode() == Module.BindMode.Hold ? 1.0f : 0.0f);
        bindModeHoverAnimation.run(bindModeBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        float progress = bindModeAnimation.getValue();
        float hoverProgress = bindModeHoverAnimation.getValue();
        float segmentWidth = bindModeBounds.width() / 2.0f;
        float indicatorWidth = segmentWidth - 4.0f;
        float indicatorX = bindModeBounds.x() + 2.0f + (segmentWidth * progress);

        drawHeaderControlSurface(bindModeBounds, hoverProgress, 0.0f);
        roundRectRenderer.addRoundRect(indicatorX, bindModeBounds.y() + 2.0f, indicatorWidth, bindModeBounds.height() - 4.0f, 7.0f, DropdownTheme.SECONDARY_CONTAINER);

        float toggleScale = 0.52f;
        float holdScale = 0.52f;
        float toggleWidth = textRenderer.getWidth("Toggle", toggleScale);
        float holdWidth = textRenderer.getWidth("Hold", holdScale);
        float textHeight = textRenderer.getHeight(toggleScale);
        float centerY = bindModeBounds.y() + (bindModeBounds.height() - textHeight) / 2.0f - 1.0f;
        Color inactiveText = DropdownTheme.isLightTheme() ? DropdownTheme.TEXT_MUTED : DropdownTheme.TEXT_SECONDARY;
        textRenderer.addText("Toggle", bindModeBounds.x() + (segmentWidth - toggleWidth) / 2.0f, centerY, toggleScale, DropdownTheme.lerp(DropdownTheme.ON_SECONDARY_CONTAINER, inactiveText, progress));
        textRenderer.addText("Hold", bindModeBounds.x() + segmentWidth + (segmentWidth - holdWidth) / 2.0f, centerY, holdScale, DropdownTheme.lerp(inactiveText, DropdownTheme.ON_SECONDARY_CONTAINER, progress));
    }

    private void drawKeybindControl(Module module, int mouseX, int mouseY) {
        DropdownLayout.Rect keybindBounds = getKeybindBounds();
        boolean listening = state.getListeningKeyBindModule() == module;
        keybindHoverAnimation.run(keybindBounds.contains(mouseX, mouseY) ? 1.0f : 0.0f);
        keybindFocusAnimation.run(listening ? 1.0f : 0.0f);
        float hoverProgress = keybindHoverAnimation.getValue();
        float focusProgress = keybindFocusAnimation.getValue();
        drawHeaderControlSurface(keybindBounds, hoverProgress, focusProgress);

        String label = listening ? "Press key" : formatKeybind(module.getKeyBind());
        float scale = 0.52f;
        float textWidth = textRenderer.getWidth(label, scale);
        float textHeight = textRenderer.getHeight(scale);
        float textX = keybindBounds.x() + (keybindBounds.width() - textWidth) / 2.0f;
        float textY = keybindBounds.y() + (keybindBounds.height() - textHeight) / 2.0f - 1.0f;
        textRenderer.addText(label, textX, textY, scale, DropdownTheme.TEXT_PRIMARY);

        if (listening) {
            float underlineWidth = Math.max(24.0f, keybindBounds.width() - 16.0f);
            float underlineX = keybindBounds.x() + (keybindBounds.width() - underlineWidth) / 2.0f;
            float underlineY = keybindBounds.bottom() - 3.0f;
            rectRenderer.addRect(underlineX, underlineY, underlineWidth, 1.5f, DropdownTheme.PRIMARY);
        }
    }

    private String formatKeybind(int keyCode) {
        if (keyCode < 0) {
            return "None";
        }
        return InputConstants.Type.KEYSYM.getOrCreate(keyCode).getDisplayName().getString();
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

    private ColorPickerPopup createColorPopup(ColorSettingRow colorRow, DropdownLayout.Rect rowBounds) {
        DropdownLayout.Rect swatchBounds = colorRow.getSwatchBounds(rowBounds);
        int channelCount = colorRow.getSetting().isAllowAlpha() ? 4 : 3;
        float popupWidth = 156.0f;
        float popupHeight = 58.0f + channelCount * 24.0f;
        float popupX = Math.max(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, swatchBounds.right() - popupWidth);
        float popupY = swatchBounds.bottom() + 4.0f;
        float maxBottom = bounds.bottom() - DropdownTheme.PANEL_VIEWPORT_INSET;
        if (popupY + popupHeight > maxBottom) {
            popupY = swatchBounds.y() - popupHeight - 4.0f;
        }
        return new ColorPickerPopup(new DropdownLayout.Rect(popupX, popupY, popupWidth, popupHeight), swatchBounds, colorRow.getSetting());
    }

    public void flushContent() {
        if (!contentPending) {
            return;
        }
        contentShadowRenderer.drawAndClear();
        contentRoundRectRenderer.drawAndClear();
        contentRectRenderer.drawAndClear();
        contentTextRenderer.drawAndClear();
        DropdownScissor.clear(contentRectRenderer, contentRoundRectRenderer, contentShadowRenderer, contentTextRenderer);
        contentPending = false;
    }

    private void clearRowFocus() {
        for (SettingRow<?> row : rowCache.values()) {
            row.setFocused(false);
        }
    }

}
