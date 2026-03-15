package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.IComponent;
import com.github.lumin.gui.clickgui.component.ModuleComponent;
import com.github.lumin.gui.clickgui.component.impl.ColorSettingComponent;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.utils.render.MouseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

final class SettingsViewController {
    private final Minecraft mc = Minecraft.getInstance();

    private final RoundRectRenderer settingsRoundRect = new RoundRectRenderer();
    private final RectRenderer settingsRect = new RectRenderer();
    private final TextRenderer settingsFont = new TextRenderer();

    private final RoundRectRenderer pickingRound = new RoundRectRenderer();
    private final RectRenderer pickingRect = new RectRenderer();
    private final RoundRectRenderer pickerRound = new RoundRectRenderer();
    private final TextRenderer pickingText = new TextRenderer();

    private final TranslateComponent searchComponent = TranslateComponent.create("gui", "search");

    private ModuleComponent settingsComponent;
    private String searchText = "";
    private boolean searchFocused = false;

    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float maxScroll = 0.0f;

    private boolean draggingScrollbar = false;
    private float scrollbarDragStartMouseY = 0.0f;
    private float scrollbarDragStartScroll = 0.0f;

    private float lastIconBoxX, lastIconBoxY, lastIconBoxW, lastIconBoxH;
    private float lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, lastSearchBoxH;
    private float lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH;
    private float lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH;
    private float lastThumbY, lastThumbH;

    private boolean exitRequested = false;

    boolean hasActiveModule() {
        return settingsComponent != null;
    }

    Module getModule() {
        return settingsComponent != null ? settingsComponent.getModule() : null;
    }

    void openModule(Module module, float sourceX, float sourceY, float sourceW, float sourceH, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        ColorSettingComponent.closeActivePicker();
        settingsComponent = new ModuleComponent(module);
        searchText = "";
        searchFocused = false;
        scrollOffset = 0.0f;
        scrollTarget = 0.0f;
        maxScroll = 0.0f;
        draggingScrollbar = false;
        exitRequested = false;

        float padding = 8 * guiScale;
        float searchHeight = 24 * guiScale;
        float targetX = panelX + padding;
        float targetY = panelY + padding + searchHeight + padding;
        float targetW = Math.max(0.0f, panelWidth * guiScale - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
        settingsComponent.setFilterText("");
        int itemCount = settingsComponent.getFilteredVisibleCount();
        float titleH = 18.0f * guiScale;
        float targetH = 8.0f * guiScale + titleH + 6.0f * guiScale + (itemCount > 0 ? itemCount * 18.0f * guiScale + Math.max(0, itemCount - 1) * 4.0f * guiScale : 0) + 8.0f * guiScale;
        settingsComponent.initAnimation(sourceX, sourceY, sourceW, sourceH, targetX, targetY, targetW, targetH);
    }

    void clearModule() {
        ColorSettingComponent.closeActivePicker();
        settingsComponent = null;
        searchText = "";
        searchFocused = false;
        scrollOffset = 0.0f;
        scrollTarget = 0.0f;
        maxScroll = 0.0f;
        draggingScrollbar = false;
        exitRequested = false;
    }

    void startExitAnimation(float sourceX, float sourceY, float sourceW, float sourceH) {
        if (settingsComponent != null) {
            settingsComponent.startExitAnimation(sourceX, sourceY, sourceW, sourceH);
        }
    }

    boolean isAnimationFinished() {
        return settingsComponent != null && settingsComponent.isAnimationFinished();
    }

    boolean consumeExitRequest() {
        boolean value = exitRequested;
        exitRequested = false;
        return value;
    }

    void render(IComponent.RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        if (settingsComponent == null) return;

        float padding = 8 * guiScale;
        float spacing = 4 * guiScale;
        float searchHeight = 24 * guiScale;
        float availableWidth = panelWidth * guiScale - padding * 2 - spacing;

        if (!settingsComponent.isExiting()) {
            lastIconBoxX = panelX + padding;
            lastIconBoxY = panelY + padding;
            lastIconBoxW = availableWidth * 0.1f;
            lastIconBoxH = searchHeight;
            renderIconBox(set, lastIconBoxX, lastIconBoxY, lastIconBoxW, searchHeight, guiScale, MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, lastIconBoxW, searchHeight, mouseX, mouseY), alpha);

            lastSearchBoxX = lastIconBoxX + lastIconBoxW + spacing;
            lastSearchBoxY = lastIconBoxY;
            lastSearchBoxW = availableWidth * 0.9f;
            lastSearchBoxH = searchHeight;
            renderSearchBox(set, lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, searchHeight, guiScale, searchFocused, MouseUtils.isHovering(lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, searchHeight, mouseX, mouseY), searchText, alpha);
        }

        lastSettingsX = panelX + padding;
        lastSettingsY = lastIconBoxY + searchHeight + padding;
        lastSettingsW = Math.max(0.0f, panelWidth * guiScale - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
        lastSettingsH = Math.max(0.0f, (panelY + panelHeight * guiScale - padding) - lastSettingsY);

        lastScrollbarX = lastSettingsX + lastSettingsW + 4.0f * guiScale;
        lastScrollbarY = lastSettingsY;
        lastScrollbarW = 4.0f * guiScale;
        lastScrollbarH = lastSettingsH;

        settingsComponent.setFilterText(searchText);
        int itemCount = settingsComponent.getFilteredVisibleCount();
        float titleH = 18.0f * guiScale;
        float contentH = 8.0f * guiScale + titleH + 6.0f * guiScale + (itemCount > 0 ? itemCount * 18.0f * guiScale + Math.max(0, itemCount - 1) * 4.0f * guiScale : 0) + 8.0f * guiScale;

        maxScroll = Math.max(0.0f, contentH - lastSettingsH);
        scrollTarget = Mth.clamp(scrollTarget, 0.0f, maxScroll);
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        scrollOffset = Mth.clamp(scrollOffset, 0.0f, maxScroll);

        lastThumbH = maxScroll <= 0.0f ? lastSettingsH : Math.max(12.0f * guiScale, lastSettingsH * (lastSettingsH / contentH));
        float thumbTravel = Math.max(0.0f, lastSettingsH - lastThumbH);
        lastThumbY = maxScroll <= 0.0f ? lastSettingsY : lastSettingsY + (scrollOffset / maxScroll) * thumbTravel;

        if (draggingScrollbar && maxScroll > 0.0f) {
            scrollTarget = handleScrollDrag(scrollTarget, maxScroll, lastThumbH, lastScrollbarH, mouseY, scrollbarDragStartMouseY, scrollbarDragStartScroll);
        }

        setupScissor(lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH, (float) mc.getWindow().getGuiScale(), mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getWindow().getGuiScaledHeight());

        IComponent.RendererSet settingsSet = new IComponent.RendererSet(settingsRoundRect, set.topRoundRect(), set.texture(), settingsFont, pickingRound, pickingRect, pickerRound, pickingText);
        if (!settingsComponent.isExiting()) {
            settingsComponent.setX(lastSettingsX);
            settingsComponent.setY(lastSettingsY - scrollOffset);
            settingsComponent.setWidth(lastSettingsW);
            settingsComponent.setHeight(contentH);
        }
        settingsComponent.render(settingsSet, mouseX, mouseY, deltaTicks);

        settingsRoundRect.drawAndClear();
        settingsRect.drawAndClear();
        settingsFont.drawAndClear();

        clearScissor();

        if (!settingsComponent.isExiting()) {
            settingsComponent.renderOverlayBlurs(mouseX, mouseY, deltaTicks);
            settingsComponent.renderOverlays(settingsSet, mouseX, mouseY, deltaTicks);
        }

        pickingRound.drawAndClear();
        pickingRect.drawAndClear();
        pickerRound.drawAndClear();
        pickingText.drawAndClear();

        if (maxScroll > 0.0f && !settingsComponent.isExiting()) {
            renderScrollbar(set, lastScrollbarX, lastSettingsY, lastScrollbarW, lastSettingsH, lastThumbY, lastThumbH, draggingScrollbar, MouseUtils.isHovering(lastScrollbarX, lastSettingsY, lastScrollbarW, lastSettingsH, mouseX, mouseY), MouseUtils.isHovering(lastScrollbarX, lastThumbY, lastScrollbarW, lastThumbH, mouseX, mouseY), alpha);
        }
    }

    boolean mouseClicked(MouseButtonEvent event, boolean focused, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        if (settingsComponent == null) return false;
        float scaledPanelWidth = panelWidth * guiScale;
        float scaledPanelHeight = panelHeight * guiScale;

        if (ColorSettingComponent.hasActivePicker() && !ColorSettingComponent.isMouseOutOfPicker((int) event.x(), (int) event.y())) {
            return settingsComponent.mouseClicked(event, focused);
        }

        if (!MouseUtils.isHovering(panelX, panelY, scaledPanelWidth, scaledPanelHeight, event.x(), event.y()))
            return false;

        if (event.button() == 0 && MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, lastIconBoxW, lastIconBoxH, event.x(), event.y())) {
            exitRequested = true;
            return true;
        }

        if (MouseUtils.isHovering(lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, lastSearchBoxH, event.x(), event.y())) {
            if (event.button() == 1) {
                searchText = "";
                scrollTarget = 0.0f;
            }
            searchFocused = true;
            return true;
        }

        searchFocused = false;

        if (event.button() == 0 && maxScroll > 0.0f && MouseUtils.isHovering(lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH, event.x(), event.y())) {
            if (MouseUtils.isHovering(lastScrollbarX, lastThumbY, lastScrollbarW, lastThumbH, event.x(), event.y())) {
                draggingScrollbar = true;
                scrollbarDragStartMouseY = (float) event.y();
                scrollbarDragStartScroll = scrollTarget;
                return true;
            }
            scrollTarget = handleScrollClick(scrollTarget, maxScroll, lastThumbH, lastThumbY, lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH, (float) event.x(), (float) event.y());
            draggingScrollbar = true;
            scrollbarDragStartMouseY = (float) event.y();
            scrollbarDragStartScroll = scrollTarget;
            return true;
        }

        return settingsComponent.mouseClicked(event, focused);
    }

    boolean mouseReleased(MouseButtonEvent event, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        draggingScrollbar = false;
        if (settingsComponent == null) return false;
        if (ColorSettingComponent.hasActivePicker() || settingsComponent.hasDraggingSetting()) {
            return settingsComponent.mouseReleased(event);
        }
        return MouseUtils.isHovering(panelX, panelY, panelWidth * guiScale, panelHeight * guiScale, event.x(), event.y()) && settingsComponent.mouseReleased(event);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (settingsComponent == null || maxScroll <= 0.0f) return false;
        scrollTarget = handleMouseScroll(scrollTarget, maxScroll, lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH, lastScrollbarW, mouseX, mouseY, scrollY);
        return MouseUtils.isHovering(lastSettingsX, lastSettingsY, lastSettingsW + lastScrollbarW, lastSettingsH, mouseX, mouseY);
    }

    boolean keyPressed(KeyEvent event) {
        if (settingsComponent == null) return false;
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                scrollTarget = 0.0f;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return settingsComponent.keyPressed(event);
    }

    boolean charTyped(CharacterEvent event) {
        if (settingsComponent == null) return false;
        if (searchFocused) {
            searchText += Character.toString(event.codepoint());
            scrollTarget = 0.0f;
            return true;
        }
        return settingsComponent.charTyped(event);
    }

    void clickOutside() {
        draggingScrollbar = false;
        searchFocused = false;
    }

    private static float handleScrollDrag(float scrollTarget, float maxScroll, float thumbH, float scrollbarH, float mouseY, float dragStartMouseY, float dragStartScroll) {
        float thumbTravel = Math.max(0.0f, scrollbarH - thumbH);
        if (thumbTravel <= 0.0f) return scrollTarget;
        float mouseDelta = mouseY - dragStartMouseY;
        return Mth.clamp(dragStartScroll + (mouseDelta / thumbTravel) * maxScroll, 0.0f, maxScroll);
    }

    private static float handleScrollClick(float scrollTarget, float maxScroll, float thumbH, float thumbY, float scrollbarX, float scrollbarY, float scrollbarW, float scrollbarH, float mouseX, float mouseY) {
        float thumbTravel = Math.max(0.0f, scrollbarH - thumbH);
        if (thumbTravel <= 0.0f) return scrollTarget;
        if (MouseUtils.isHovering(scrollbarX, thumbY, scrollbarW, thumbH, mouseX, mouseY)) return scrollTarget;
        float ratio = Mth.clamp((mouseY - scrollbarY - thumbH / 2.0f) / thumbTravel, 0.0f, 1.0f);
        return ratio * maxScroll;
    }

    private static float handleMouseScroll(float scrollTarget, float maxScroll, float areaX, float areaY, float areaW, float areaH, float scrollbarW, double mouseX, double mouseY, double scrollY) {
        if (maxScroll <= 0.0f) return scrollTarget;
        if (!MouseUtils.isHovering(areaX, areaY, areaW + scrollbarW, areaH, mouseX, mouseY)) return scrollTarget;
        float step = 24.0f * ClickGui.INSTANCE.scale.getValue().floatValue();
        return Mth.clamp(scrollTarget - (float) scrollY * step, 0.0f, maxScroll);
    }

    private void setupScissor(float areaX, float areaY, float areaW, float areaH, float pxScale, int fbW, int fbH, int guiH) {
        int scX = Mth.floor(areaX * pxScale);
        int scY = Mth.floor((guiH - (areaY + areaH)) * pxScale);
        int scW = Mth.ceil(areaW * pxScale);
        int scH = Mth.ceil(areaH * pxScale);
        scX = Mth.clamp(scX, 0, fbW);
        scY = Mth.clamp(scY, 0, fbH);
        scW = Mth.clamp(scW, 0, fbW - scX);
        scH = Mth.clamp(scH, 0, fbH - scY);
        settingsRoundRect.setScissor(scX, scY, scW, scH);
        settingsRect.setScissor(scX, scY, scW, scH);
        settingsFont.setScissor(scX, scY, scW, scH);
    }

    private void clearScissor() {
        settingsRoundRect.clearScissor();
        settingsRect.clearScissor();
        settingsFont.clearScissor();
    }

    private void renderSearchBox(IComponent.RendererSet set, float x, float y, float w, float h, float guiScale, boolean focused, boolean hovered, String text, float alpha) {
        Color bgColor = focused ? applyAlpha(new Color(50, 50, 50, 200), alpha) : (hovered ? applyAlpha(new Color(40, 40, 40, 200), alpha) : applyAlpha(new Color(30, 30, 30, 200), alpha));
        set.bottomRoundRect().addRoundRect(x, y, w, h, 8f * guiScale, bgColor);
        String display = text.isEmpty() && !focused ? searchComponent.getTranslatedName() : text;
        if (focused && (System.currentTimeMillis() % 1000 > 500)) display += "_";
        set.font().addText(display, x + 6 * guiScale, y + h / 2 - 7 * guiScale, guiScale * 0.9f, text.isEmpty() && !focused ? applyAlpha(Color.GRAY, alpha) : applyAlpha(Color.WHITE, alpha));
    }

    private static void renderIconBox(IComponent.RendererSet set, float x, float y, float w, float h, float guiScale, boolean hovered, float alpha) {
        set.bottomRoundRect().addRoundRect(x, y, w, h, 8f * guiScale, hovered ? applyAlpha(new Color(40, 40, 40, 200), alpha) : applyAlpha(new Color(30, 30, 30, 200), alpha));
        float iconScale = guiScale * 1.2f;
        float iconW = set.font().getWidth("<", iconScale, StaticFontLoader.ICONS);
        float iconH = set.font().getHeight(iconScale, StaticFontLoader.ICONS);
        float iconX = x + (w - iconW) / 2f;
        float iconY = y + (h - iconH) / 2f - guiScale;
        set.font().addText("<", iconX, iconY - 1, iconScale, applyAlpha(new Color(200, 200, 200), alpha), StaticFontLoader.ICONS);
    }

    private static void renderScrollbar(IComponent.RendererSet set, float x, float y, float w, float h, float thumbY, float thumbH, boolean dragging, boolean hovered, boolean thumbHovered, float alpha) {
        Color trackColor = hovered ? applyAlpha(new Color(255, 255, 255, 28), alpha) : applyAlpha(new Color(255, 255, 255, 18), alpha);
        Color thumbColor = dragging ? applyAlpha(new Color(255, 255, 255, 90), alpha) : (thumbHovered ? applyAlpha(new Color(255, 255, 255, 75), alpha) : applyAlpha(new Color(255, 255, 255, 55), alpha));
        set.bottomRoundRect().addRoundRect(x, y, w, h, w / 2.0f, trackColor);
        set.bottomRoundRect().addRoundRect(x, thumbY, w, thumbH, w / 2.0f, thumbColor);
    }

    private static Color applyAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha));
    }
}
