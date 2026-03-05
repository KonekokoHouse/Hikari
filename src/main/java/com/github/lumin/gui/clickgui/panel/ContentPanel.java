package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.graphics.renderers.BlurRenderer;
import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.IComponent;
import com.github.lumin.gui.clickgui.component.ModuleComponent;
import com.github.lumin.gui.clickgui.component.impl.ColorSettingComponent;
import com.github.lumin.managers.Managers;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ContentPanel implements IComponent {

    private final Minecraft mc = Minecraft.getInstance();

    private float x;
    private float y;
    private float width;
    private float height;
    private Category currentCategory;

    private final Animation viewAnimation = new Animation(Easing.EASE_OUT_EXPO, 450L);
    private float sourceCardX, sourceCardY, sourceCardW, sourceCardH;
    private boolean closeSettingsRequested;
    private boolean exitAnimationStarted;
    private int currentState = 0;
    private int targetState = 0;

    private static final float CARD_ASPECT_WIDTH = 16.0f;
    private static final float CARD_ASPECT_HEIGHT = 9.0f;

    private final RoundRectRenderer listRoundRect = new RoundRectRenderer();
    private final TextRenderer listFont = new TextRenderer();
    private final List<ModuleCard> moduleCards = new ArrayList<>();

    private String listSearchText = "";
    private boolean listSearchFocused = false;
    private float listScrollOffset = 0.0f;
    private float listScrollTarget = 0.0f;
    private float listMaxScroll = 0.0f;
    private boolean listDraggingScrollbar = false;
    private float listScrollbarDragStartMouseY = 0.0f;
    private float listScrollbarDragStartScroll = 0.0f;

    private float lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, lastSearchBoxH;
    private float lastListX, lastListY, lastListW, lastListH;
    private float lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH;
    private float lastThumbY, lastThumbH;

    private Module requestedSettingsModule = null;

    private final RoundRectRenderer settingsRoundRect = new RoundRectRenderer();
    private final RectRenderer settingsRect = new RectRenderer();
    private final TextRenderer settingsFont = new TextRenderer();

    private final RoundRectRenderer pickingRound = new RoundRectRenderer();
    private final RectRenderer pickingRect = new RectRenderer();
    private final RoundRectRenderer pickerRound = new RoundRectRenderer();
    private final TextRenderer pickingText = new TextRenderer();

    private ModuleComponent settingsComponent = null;
    private String settingsSearchText = "";
    private boolean settingsSearchFocused = false;

    private float settingsScrollOffset = 0.0f;
    private float settingsScrollTarget = 0.0f;
    private float settingsMaxScroll = 0.0f;

    private boolean settingsDraggingScrollbar = false;
    private float settingsScrollbarDragStartMouseY = 0.0f;
    private float settingsScrollbarDragStartScroll = 0.0f;

    private float lastIconBoxX, lastIconBoxY, lastIconBoxW, lastIconBoxH;
    private float lastSettingsSearchBoxX, lastSettingsSearchBoxY, lastSettingsSearchBoxW, lastSettingsSearchBoxH;
    private float lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH;
    private float lastSettingsScrollbarX, lastSettingsScrollbarY, lastSettingsScrollbarW, lastSettingsScrollbarH;
    private float lastSettingsThumbY, lastSettingsThumbH;

    private boolean settingsExitRequested = false;

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setCurrentCategory(Category category) {
        if (this.currentCategory == category) return;
        this.currentCategory = category;
        this.closeSettingsRequested = false;
        clearSettingsModule();
        this.currentState = 0;
        this.targetState = 0;
        this.viewAnimation.setStartValue(0.0f);
        List<Module> modules = new ArrayList<>();
        for (Module module : Managers.MODULE.getModules()) {
            if (module.category == category) {
                modules.add(module);
            }
        }
        setModules(modules);
    }

    private void setModules(List<Module> modules) {
        moduleCards.clear();
        for (Module module : modules) {
            moduleCards.add(new ModuleCard(module));
        }
        listSearchText = "";
        listSearchFocused = false;
        listScrollOffset = 0.0f;
        listScrollTarget = 0.0f;
        listMaxScroll = 0.0f;
        listDraggingScrollbar = false;
        requestedSettingsModule = null;
    }

    private Module consumeRequestedSettingsModule() {
        Module m = requestedSettingsModule;
        requestedSettingsModule = null;
        return m;
    }

    private void renderSearchBox(RendererSet set, float x, float y, float w, float h, float guiScale, boolean focused, boolean hovered, String text, float alpha) {
        Color bgColor = focused ? applyAlpha(new Color(50, 50, 50, 200), alpha) : (hovered ? applyAlpha(new Color(40, 40, 40, 200), alpha) : applyAlpha(new Color(30, 30, 30, 200), alpha));
        set.bottomRoundRect().addRoundRect(x, y, w, h, 8f * guiScale, bgColor);
        String display = text.isEmpty() && !focused ? "搜索..." : text;
        if (focused && (System.currentTimeMillis() % 1000 > 500)) display += "_";
        set.font().addText(display, x + 6 * guiScale, y + h / 2 - 7 * guiScale, guiScale * 0.9f, text.isEmpty() && !focused ? applyAlpha(Color.GRAY, alpha) : applyAlpha(Color.WHITE, alpha));
    }

    private void renderIconBox(RendererSet set, float x, float y, float w, float h, float guiScale, boolean hovered, float alpha) {
        set.bottomRoundRect().addRoundRect(x, y, w, h, 8f * guiScale, hovered ? applyAlpha(new Color(40, 40, 40, 200), alpha) : applyAlpha(new Color(30, 30, 30, 200), alpha));
        float iconScale = guiScale * 1.2f;
        float iconW = set.font().getWidth("<", iconScale, StaticFontLoader.ICONS);
        float iconH = set.font().getHeight(iconScale, StaticFontLoader.ICONS);
        float iconX = x + (w - iconW) / 2f;
        float iconY = y + (h - iconH) / 2f - guiScale;
        set.font().addText("<", iconX, iconY - 1, iconScale, applyAlpha(new Color(200, 200, 200), alpha), StaticFontLoader.ICONS);
    }

    private void renderScrollbar(RendererSet set, float x, float y, float w, float h, float thumbY, float thumbH, boolean dragging, boolean hovered, boolean thumbHovered, float alpha) {
        Color trackColor = hovered ? applyAlpha(new Color(255, 255, 255, 28), alpha) : applyAlpha(new Color(255, 255, 255, 18), alpha);
        Color thumbColor = dragging ? applyAlpha(new Color(255, 255, 255, 90), alpha) : (thumbHovered ? applyAlpha(new Color(255, 255, 255, 75), alpha) : applyAlpha(new Color(255, 255, 255, 55), alpha));
        set.bottomRoundRect().addRoundRect(x, y, w, h, w / 2.0f, trackColor);
        set.bottomRoundRect().addRoundRect(x, thumbY, w, thumbH, w / 2.0f, thumbColor);
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

    private float handleScrollDrag(float scrollTarget, float maxScroll, float thumbH, float scrollbarH, float mouseY, float dragStartMouseY, float dragStartScroll) {
        float thumbTravel = Math.max(0.0f, scrollbarH - thumbH);
        if (thumbTravel <= 0.0f) return scrollTarget;
        float mouseDelta = mouseY - dragStartMouseY;
        return Mth.clamp(dragStartScroll + (mouseDelta / thumbTravel) * maxScroll, 0.0f, maxScroll);
    }

    private float handleScrollClick(float scrollTarget, float maxScroll, float thumbH, float thumbY, float scrollbarX, float scrollbarY, float scrollbarW, float scrollbarH, float mouseX, float mouseY) {
        float thumbTravel = Math.max(0.0f, scrollbarH - thumbH);
        if (thumbTravel <= 0.0f) return scrollTarget;
        if (MouseUtils.isHovering(scrollbarX, thumbY, scrollbarW, thumbH, mouseX, mouseY)) return scrollTarget;
        float ratio = Mth.clamp((mouseY - scrollbarY - thumbH / 2.0f) / thumbTravel, 0.0f, 1.0f);
        return ratio * maxScroll;
    }

    private float handleMouseScroll(float scrollTarget, float maxScroll, float areaX, float areaY, float areaW, float areaH, float scrollbarW, double mouseX, double mouseY, double scrollY) {
        if (maxScroll <= 0.0f) return scrollTarget;
        if (!MouseUtils.isHovering(areaX, areaY, areaW + scrollbarW, areaH, mouseX, mouseY)) return scrollTarget;
        float step = 24.0f * ClickGui.INSTANCE.scale.getValue().floatValue();
        return Mth.clamp(scrollTarget - (float) scrollY * step, 0.0f, maxScroll);
    }

    private boolean handleSearchKey(KeyEvent event, StringBuilder searchText, Runnable onClear) {
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText.deleteCharAt(searchText.length() - 1);
                onClear.run();
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
            return true;
        }
        return false;
    }

    private void renderListView(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float padding = 8 * guiScale;
        float spacing = 4 * guiScale;
        float searchHeight = 24 * guiScale;
        float iconBoxWidth = (this.width * guiScale - padding * 2 - spacing) * 0.1f;
        float searchBoxWidth = (this.width * guiScale - padding * 2 - spacing) * 0.9f;

        lastIconBoxX = this.x + padding;
        lastIconBoxY = this.y + padding;
        lastIconBoxW = iconBoxWidth;
        lastIconBoxH = searchHeight;

        renderIconBox(set, lastIconBoxX, lastIconBoxY, iconBoxWidth, searchHeight, guiScale, MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, iconBoxWidth, searchHeight, mouseX, mouseY), alpha);

        lastSearchBoxX = lastIconBoxX + iconBoxWidth + spacing;
        lastSearchBoxY = lastIconBoxY;
        lastSearchBoxW = searchBoxWidth;
        lastSearchBoxH = searchHeight;
        renderSearchBox(set, lastSearchBoxX, lastSearchBoxY, searchBoxWidth, searchHeight, guiScale, listSearchFocused, MouseUtils.isHovering(lastSearchBoxX, lastSearchBoxY, searchBoxWidth, searchHeight, mouseX, mouseY), listSearchText, alpha);

        lastListX = this.x + padding;
        lastListY = lastIconBoxY + searchHeight + padding;
        lastListW = Math.max(0.0f, this.width * guiScale - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
        lastListH = Math.max(0.0f, this.y + this.height * guiScale - padding - lastListY);

        lastScrollbarX = lastListX + lastListW + 4.0f * guiScale;
        lastScrollbarY = lastListY;
        lastScrollbarW = 4.0f * guiScale;
        lastScrollbarH = lastListH;

        List<ModuleCard> visibleCards = new ArrayList<>();
        for (ModuleCard card : moduleCards) {
            boolean matchesSearch = listSearchText.isEmpty() || card.module.getName().toLowerCase().startsWith(listSearchText.toLowerCase());
            card.updateVisibility(matchesSearch);
            if (!matchesSearch && card.scaleAnimation.getValue() <= 0.01f) {
                card.width = 0;
                card.height = 0;
                continue;
            }
            visibleCards.add(card);
        }

        if (visibleCards.isEmpty() || lastListH <= 0.0f || lastListW <= 0.0f) {
            listMaxScroll = listScrollOffset = listScrollTarget = 0.0f;
            listDraggingScrollbar = false;
            lastThumbY = lastThumbH = 0.0f;
            return;
        }

        float itemGap = 8 * guiScale;
        int columns = Math.max(3, Mth.floor((lastListW + itemGap) / (120 * guiScale + itemGap)));
        float cardWidth = (lastListW - itemGap * (columns - 1)) / columns;
        float cardHeight = cardWidth * (CARD_ASPECT_HEIGHT / CARD_ASPECT_WIDTH);
        int totalRows = Mth.ceil(visibleCards.size() / (double) columns);
        float contentH = totalRows * cardHeight + Math.max(0, totalRows - 1) * itemGap;

        listMaxScroll = Math.max(0.0f, contentH - lastListH);
        listScrollTarget = Mth.clamp(listScrollTarget, 0.0f, listMaxScroll);
        listScrollOffset += (listScrollTarget - listScrollOffset) * 0.35f;
        listScrollOffset = Mth.clamp(listScrollOffset, 0.0f, listMaxScroll);

        lastThumbH = listMaxScroll <= 0.0f ? lastListH : Math.max(12.0f * guiScale, lastListH * (lastListH / contentH));
        float thumbTravel = Math.max(0.0f, lastListH - lastThumbH);
        lastThumbY = listMaxScroll <= 0.0f ? lastListY : lastListY + (listScrollOffset / listMaxScroll) * thumbTravel;

        if (listDraggingScrollbar && listMaxScroll > 0.0f) {
            listScrollTarget = handleScrollDrag(listScrollTarget, listMaxScroll, lastThumbH, lastScrollbarH, mouseY, listScrollbarDragStartMouseY, listScrollbarDragStartScroll);
        }

        float pxScale = (float) mc.getWindow().getGuiScale();
        int scX = Mth.clamp(Mth.floor(lastListX * pxScale), 0, mc.getWindow().getWidth());
        int scY = Mth.clamp(Mth.floor((mc.getWindow().getGuiScaledHeight() - (lastListY + lastListH)) * pxScale), 0, mc.getWindow().getHeight());
        int scW = Mth.clamp(Mth.ceil(lastListW * pxScale), 0, mc.getWindow().getWidth() - scX);
        int scH = Mth.clamp(Mth.ceil(lastListH * pxScale), 0, mc.getWindow().getHeight() - scY);
        listRoundRect.setScissor(scX, scY, scW, scH);
        listFont.setScissor(scX, scY, scW, scH);

        float listBottom = this.y + this.height * guiScale - padding;
        int visibleIndex = 0;
        Module expandingModule = settingsComponent != null ? settingsComponent.getModule() : null;

        for (ModuleCard card : moduleCards) {
            boolean matchesSearch = listSearchText.isEmpty() || card.module.getName().toLowerCase().startsWith(listSearchText.toLowerCase());
            card.updateVisibility(matchesSearch);
            if (!matchesSearch && card.scaleAnimation.getValue() <= 0.01f) {
                card.width = 0;
                card.height = 0;
                continue;
            }
            int row = visibleIndex / columns;
            int col = visibleIndex % columns;
            card.x = lastListX + col * (cardWidth + itemGap);
            card.y = lastListY + row * (cardHeight + itemGap) - listScrollOffset;
            card.width = cardWidth;
            card.height = cardHeight;
            if (card.shouldRender() && card.y + cardHeight >= lastListY && card.y <= listBottom) {
                if (expandingModule != card.module || currentState == 0) {
                    card.render(listRoundRect, listFont, mouseX, mouseY, guiScale, alpha);
                }
            }
            visibleIndex++;
        }

        listRoundRect.drawAndClear();
        listFont.drawAndClear();
        listRoundRect.clearScissor();
        listFont.clearScissor();

        if (listMaxScroll > 0.0f) {
            renderScrollbar(set, lastScrollbarX, lastListY, lastScrollbarW, lastListH, lastThumbY, lastThumbH, listDraggingScrollbar, MouseUtils.isHovering(lastScrollbarX, lastListY, lastScrollbarW, lastListH, mouseX, mouseY), MouseUtils.isHovering(lastScrollbarX, lastThumbY, lastScrollbarW, lastThumbH, mouseX, mouseY), alpha);
        }
    }

    private boolean listViewMouseClicked(MouseButtonEvent event, boolean focused) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float panelWidth = this.width * guiScale;
        float panelHeight = this.height * guiScale;

        if (!MouseUtils.isHovering(x, y, panelWidth, panelHeight, event.x(), event.y())) return false;

        if (MouseUtils.isHovering(lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, lastSearchBoxH, event.x(), event.y())) {
            if (event.button() == 1) {
                listSearchText = "";
                listScrollTarget = 0.0f;
            }
            listSearchFocused = true;
            return true;
        }

        listSearchFocused = false;

        if (event.button() == 0 && listMaxScroll > 0.0f && MouseUtils.isHovering(lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH, event.x(), event.y())) {
            if (MouseUtils.isHovering(lastScrollbarX, lastThumbY, lastScrollbarW, lastThumbH, event.x(), event.y())) {
                listDraggingScrollbar = true;
                listScrollbarDragStartMouseY = (float) event.y();
                listScrollbarDragStartScroll = listScrollTarget;
                return true;
            }
            listScrollTarget = handleScrollClick(listScrollTarget, listMaxScroll, lastThumbH, lastThumbY, lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH, (float) event.x(), (float) event.y());
            listDraggingScrollbar = true;
            listScrollbarDragStartMouseY = (float) event.y();
            listScrollbarDragStartScroll = listScrollTarget;
            return true;
        }

        if (event.button() == 0) {
            for (ModuleCard card : moduleCards) {
                if (card.width > 0 && card.height > 0 && MouseUtils.isHovering(card.x, card.y, card.width, card.height, event.x(), event.y())) {
                    card.module.toggle();
                    return true;
                }
            }
        }

        if (event.button() == 1) {
            for (ModuleCard card : moduleCards) {
                if (card.width > 0 && card.height > 0 && MouseUtils.isHovering(card.x, card.y, card.width, card.height, event.x(), event.y())) {
                    requestedSettingsModule = card.module;
                    sourceCardX = card.getRenderX();
                    sourceCardY = card.getRenderY();
                    sourceCardW = card.getRenderW();
                    sourceCardH = card.getRenderH();
                    listDraggingScrollbar = false;
                    return true;
                }
            }
        }

        return true;
    }

    private boolean listViewMouseReleased(MouseButtonEvent event) {
        listDraggingScrollbar = false;
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        return MouseUtils.isHovering(x, y, this.width * guiScale, this.height * guiScale, event.x(), event.y());
    }

    private boolean listViewMouseScrolled(double mouseX, double mouseY, double scrollY) {
        listScrollTarget = handleMouseScroll(listScrollTarget, listMaxScroll, lastListX, lastListY, lastListW, lastListH, lastScrollbarW, mouseX, mouseY, scrollY);
        return listMaxScroll > 0.0f && MouseUtils.isHovering(lastListX, lastListY, lastListW + lastScrollbarW, lastListH, mouseX, mouseY);
    }

    private boolean listViewKeyPressed(KeyEvent event) {
        if (!listSearchFocused) return false;
        if (handleSearchKey(event, new StringBuilder(listSearchText), () -> {
            listSearchText = "";
            listScrollTarget = 0.0f;
        })) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !listSearchText.isEmpty()) {
                listSearchText = listSearchText.substring(0, listSearchText.length() - 1);
                listScrollTarget = 0.0f;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) listSearchFocused = false;
            return true;
        }
        return false;
    }

    private boolean listViewCharTyped(CharacterEvent event) {
        if (!listSearchFocused) return false;
        listSearchText += Character.toString(event.codepoint());
        listScrollTarget = 0.0f;
        return true;
    }

    private void listViewClickOutside() {
        listSearchFocused = false;
        listDraggingScrollbar = false;
    }

    private boolean isSettingsActive() {
        return settingsComponent != null;
    }

    private void setSettingsModule(Module module) {
        ColorSettingComponent.closeActivePicker();
        settingsComponent = new ModuleComponent(module);
        settingsSearchText = "";
        settingsSearchFocused = false;
        settingsScrollOffset = 0.0f;
        settingsScrollTarget = 0.0f;
        settingsMaxScroll = 0.0f;
        settingsDraggingScrollbar = false;
        settingsExitRequested = false;
    }

    private void setSettingsModuleWithSource(Module module, float cardX, float cardY, float cardW, float cardH) {
        this.sourceCardX = cardX;
        this.sourceCardY = cardY;
        this.sourceCardW = cardW;
        this.sourceCardH = cardH;
        setSettingsModule(module);
    }

    private void clearSettingsModule() {
        ColorSettingComponent.closeActivePicker();
        settingsComponent = null;
        settingsSearchText = "";
        settingsSearchFocused = false;
        settingsScrollOffset = 0.0f;
        settingsScrollTarget = 0.0f;
        settingsMaxScroll = 0.0f;
        settingsDraggingScrollbar = false;
        settingsExitRequested = false;
    }

    private boolean consumeSettingsExitRequest() {
        boolean v = settingsExitRequested;
        settingsExitRequested = false;
        return v;
    }

    private void renderSettingsView(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {
        if (settingsComponent == null) return;

        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float padding = 8 * guiScale;
        float spacing = 4 * guiScale;
        float searchHeight = 24 * guiScale;
        float availableWidth = this.width * guiScale - padding * 2 - spacing;

        if (!settingsComponent.isExiting()) {
            lastIconBoxX = this.x + padding;
            lastIconBoxY = this.y + padding;
            lastIconBoxW = availableWidth * 0.1f;
            lastIconBoxH = searchHeight;
            renderIconBox(set, lastIconBoxX, lastIconBoxY, lastIconBoxW, searchHeight, guiScale, MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, lastIconBoxW, searchHeight, mouseX, mouseY), alpha);

            lastSettingsSearchBoxX = lastIconBoxX + lastIconBoxW + spacing;
            lastSettingsSearchBoxY = lastIconBoxY;
            lastSettingsSearchBoxW = availableWidth * 0.9f;
            lastSettingsSearchBoxH = searchHeight;
            renderSearchBox(set, lastSettingsSearchBoxX, lastSettingsSearchBoxY, lastSettingsSearchBoxW, searchHeight, guiScale, settingsSearchFocused, MouseUtils.isHovering(lastSettingsSearchBoxX, lastSettingsSearchBoxY, lastSettingsSearchBoxW, searchHeight, mouseX, mouseY), settingsSearchText, alpha);
        }

        lastSettingsX = this.x + padding;
        lastSettingsY = lastIconBoxY + searchHeight + padding;
        lastSettingsW = Math.max(0.0f, this.width * guiScale - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
        lastSettingsH = Math.max(0.0f, (this.y + this.height * guiScale - padding) - lastSettingsY);

        lastSettingsScrollbarX = lastSettingsX + lastSettingsW + 4.0f * guiScale;
        lastSettingsScrollbarY = lastSettingsY;
        lastSettingsScrollbarW = 4.0f * guiScale;
        lastSettingsScrollbarH = lastSettingsH;

        settingsComponent.setFilterText(settingsSearchText);
        int itemCount = settingsComponent.getFilteredVisibleCount();
        float titleH = set.font().getHeight(1.15f * guiScale);
        float contentH = 8.0f * guiScale + titleH + 6.0f * guiScale + (itemCount > 0 ? itemCount * 18.0f * guiScale + Math.max(0, itemCount - 1) * 4.0f * guiScale : 0) + 8.0f * guiScale;

        settingsMaxScroll = Math.max(0.0f, contentH - lastSettingsH);
        settingsScrollTarget = Mth.clamp(settingsScrollTarget, 0.0f, settingsMaxScroll);
        settingsScrollOffset += (settingsScrollTarget - settingsScrollOffset) * 0.35f;
        settingsScrollOffset = Mth.clamp(settingsScrollOffset, 0.0f, settingsMaxScroll);

        lastSettingsThumbH = settingsMaxScroll <= 0.0f ? lastSettingsH : Math.max(12.0f * guiScale, lastSettingsH * (lastSettingsH / contentH));
        float thumbTravel = Math.max(0.0f, lastSettingsH - lastSettingsThumbH);
        lastSettingsThumbY = settingsMaxScroll <= 0.0f ? lastSettingsY : lastSettingsY + (settingsScrollOffset / settingsMaxScroll) * thumbTravel;

        if (settingsDraggingScrollbar && settingsMaxScroll > 0.0f) {
            settingsScrollTarget = handleScrollDrag(settingsScrollTarget, settingsMaxScroll, lastSettingsThumbH, lastSettingsScrollbarH, mouseY, settingsScrollbarDragStartMouseY, settingsScrollbarDragStartScroll);
        }

        setupScissor(lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH, (float) mc.getWindow().getGuiScale(), mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getWindow().getGuiScaledHeight());

        RendererSet settingsSet = new RendererSet(settingsRoundRect, set.topRoundRect(), set.texture(), settingsFont, pickingRound, pickingRect, pickerRound, pickingText);
        if (!settingsComponent.isExiting()) {
            settingsComponent.setX(lastSettingsX);
            settingsComponent.setY(lastSettingsY - settingsScrollOffset);
            settingsComponent.setWidth(lastSettingsW);
            settingsComponent.setHeight(contentH);
        }
        settingsComponent.render(settingsSet, mouseX, mouseY, deltaTicks);

        settingsRoundRect.drawAndClear();
        settingsRect.drawAndClear();
        settingsFont.drawAndClear();
        clearScissor();

        settingsComponent.renderOverlayBlurs(mouseX, mouseY, deltaTicks);
        settingsComponent.renderOverlays(settingsSet, mouseX, mouseY, deltaTicks);

        pickingRound.drawAndClear();
        pickingRect.drawAndClear();
        pickerRound.drawAndClear();
        pickingText.drawAndClear();

        if (settingsMaxScroll > 0.0f && !settingsComponent.isExiting()) {
            renderScrollbar(set, lastSettingsScrollbarX, lastSettingsY, lastSettingsScrollbarW, lastSettingsH, lastSettingsThumbY, lastSettingsThumbH, settingsDraggingScrollbar, MouseUtils.isHovering(lastSettingsScrollbarX, lastSettingsY, lastSettingsScrollbarW, lastSettingsH, mouseX, mouseY), MouseUtils.isHovering(lastSettingsScrollbarX, lastSettingsThumbY, lastSettingsScrollbarW, lastSettingsThumbH, mouseX, mouseY), alpha);
        }
    }

    private boolean settingsViewMouseClicked(MouseButtonEvent event, boolean focused) {
        if (settingsComponent == null) return false;
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float panelWidth = this.width * guiScale;
        float panelHeight = this.height * guiScale;

        if (ColorSettingComponent.hasActivePicker() && !ColorSettingComponent.isMouseOutOfPicker((int) event.x(), (int) event.y())) {
            return settingsComponent.mouseClicked(event, focused);
        }

        if (!MouseUtils.isHovering(x, y, panelWidth, panelHeight, event.x(), event.y())) return false;

        if (event.button() == 0 && MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, lastIconBoxW, lastIconBoxH, event.x(), event.y())) {
            settingsExitRequested = true;
            return true;
        }

        if (MouseUtils.isHovering(lastSettingsSearchBoxX, lastSettingsSearchBoxY, lastSettingsSearchBoxW, lastSettingsSearchBoxH, event.x(), event.y())) {
            if (event.button() == 1) {
                settingsSearchText = "";
                settingsScrollTarget = 0.0f;
            }
            settingsSearchFocused = true;
            return true;
        }

        settingsSearchFocused = false;

        if (event.button() == 0 && settingsMaxScroll > 0.0f && MouseUtils.isHovering(lastSettingsScrollbarX, lastSettingsScrollbarY, lastSettingsScrollbarW, lastSettingsScrollbarH, event.x(), event.y())) {
            if (MouseUtils.isHovering(lastSettingsScrollbarX, lastSettingsThumbY, lastSettingsScrollbarW, lastSettingsThumbH, event.x(), event.y())) {
                settingsDraggingScrollbar = true;
                settingsScrollbarDragStartMouseY = (float) event.y();
                settingsScrollbarDragStartScroll = settingsScrollTarget;
                return true;
            }
            settingsScrollTarget = handleScrollClick(settingsScrollTarget, settingsMaxScroll, lastSettingsThumbH, lastSettingsThumbY, lastSettingsScrollbarX, lastSettingsScrollbarY, lastSettingsScrollbarW, lastSettingsScrollbarH, (float) event.x(), (float) event.y());
            settingsDraggingScrollbar = true;
            settingsScrollbarDragStartMouseY = (float) event.y();
            settingsScrollbarDragStartScroll = settingsScrollTarget;
            return true;
        }

        return settingsComponent.mouseClicked(event, focused);
    }

    private boolean settingsViewMouseReleased(MouseButtonEvent event) {
        settingsDraggingScrollbar = false;
        if (settingsComponent == null) return false;
        if (ColorSettingComponent.hasActivePicker() || settingsComponent.hasDraggingSetting()) {
            return settingsComponent.mouseReleased(event);
        }
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        return MouseUtils.isHovering(x, y, this.width * guiScale, this.height * guiScale, event.x(), event.y()) && settingsComponent.mouseReleased(event);
    }

    private boolean settingsViewMouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (settingsComponent == null || settingsMaxScroll <= 0.0f) return false;
        settingsScrollTarget = handleMouseScroll(settingsScrollTarget, settingsMaxScroll, lastSettingsX, lastSettingsY, lastSettingsW, lastSettingsH, lastSettingsScrollbarW, mouseX, mouseY, scrollY);
        return MouseUtils.isHovering(lastSettingsX, lastSettingsY, lastSettingsW + lastSettingsScrollbarW, lastSettingsH, mouseX, mouseY);
    }

    private boolean settingsViewKeyPressed(KeyEvent event) {
        if (settingsComponent == null) return false;
        if (settingsSearchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !settingsSearchText.isEmpty()) {
                settingsSearchText = settingsSearchText.substring(0, settingsSearchText.length() - 1);
                settingsScrollTarget = 0.0f;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                settingsSearchFocused = false;
                return true;
            }
        }
        return settingsComponent.keyPressed(event);
    }

    private boolean settingsViewCharTyped(CharacterEvent event) {
        if (settingsComponent == null) return false;
        if (settingsSearchFocused) {
            settingsSearchText += Character.toString(event.codepoint());
            settingsScrollTarget = 0.0f;
            return true;
        }
        return settingsComponent.charTyped(event);
    }

    private void settingsViewClickOutside() {
        settingsDraggingScrollbar = false;
        settingsSearchFocused = false;
    }

    private static final class ModuleCard {
        float x, y, width, height;
        final Module module;
        private final Animation hoverAnimation = new Animation(Easing.EASE_OUT_QUAD, 120L);
        private final Animation enabledAnimation = new Animation(Easing.EASE_OUT_QUAD, 160L);
        private final Animation scaleAnimation = new Animation(Easing.EASE_OUT_EXPO, 350L);
        private boolean wasVisible = false;
        private boolean isAnimatingExit = false;

        private ModuleCard(Module module) {
            this.module = module;
            enabledAnimation.setStartValue(module.isEnabled() ? 1.0f : 0.0f);
            scaleAnimation.setStartValue(0.0f);
            scaleAnimation.run(1.0f);
        }

        private void updateVisibility(boolean visible) {
            if (visible && !wasVisible) {
                isAnimatingExit = false;
                scaleAnimation.setStartValue(0.0f);
                scaleAnimation.run(1.0f);
            } else if (!visible && wasVisible) {
                isAnimatingExit = true;
                scaleAnimation.setStartValue(1.0f);
                scaleAnimation.run(0.0f);
            }
            wasVisible = visible;
        }

        private boolean shouldRender() {
            return scaleAnimation.getValue() > 0.01f || !isAnimatingExit;
        }

        private float getRenderX() {
            float rw = width;
            float centerX = x + width / 2.0f;
            return centerX - rw / 2.0f;
        }

        private float getRenderY() {
            float rh = height;
            float centerY = y + height / 2.0f;
            return centerY - rh / 2.0f;
        }

        private float getRenderW() {
            return width;
        }

        private float getRenderH() {
            return height;
        }

        private void render(RoundRectRenderer round, TextRenderer text, int mouseX, int mouseY, float guiScale, float alpha) {
            if (width <= 0 || height <= 0) return;

            scaleAnimation.run(isAnimatingExit ? 0.0f : 1.0f);
            float scaleProgress = Mth.clamp(scaleAnimation.getValue(), 0.0f, 1.0f);
            if (scaleProgress <= 0.01f) return;

            boolean hovered = MouseUtils.isHovering(x, y, width, height, mouseX, mouseY);
            hoverAnimation.run(hovered ? 1.0f : 0.0f);
            enabledAnimation.run(module.isEnabled() ? 1.0f : 0.0f);
            float ht = Mth.clamp(hoverAnimation.getValue(), 0.0f, 1.0f);
            float et = Mth.clamp(enabledAnimation.getValue(), 0.0f, 1.0f);

            Color offColor = new Color(40, 40, 40, 130);
            Color onColor = new Color(148, 148, 148, 130);
            int r = (int) (offColor.getRed() + (onColor.getRed() - offColor.getRed()) * et);
            int g = (int) (offColor.getGreen() + (onColor.getGreen() - offColor.getGreen()) * et);
            int b = (int) (offColor.getBlue() + (onColor.getBlue() - offColor.getBlue()) * et);
            int a = Mth.clamp((int) (offColor.getAlpha() + (onColor.getAlpha() - offColor.getAlpha()) * et) + (int) (24.0f * ht), 0, 255);

            float baseScale = 0.5f + 0.5f * scaleProgress;
            float hoverScale = 1.0f + 0.02f * ht;
            float totalScale = baseScale * hoverScale;
            float rw = width * totalScale;
            float rh = height * totalScale;
            float centerX = x + width / 2.0f;
            float centerY = y + height / 2.0f;
            float renderX = centerX - rw / 2.0f;
            float renderY = centerY - rh / 2.0f;

            int animAlpha = (int) (a * alpha * scaleProgress);
            round.addRoundRect(renderX, renderY, rw, rh, 10f * guiScale * totalScale, new Color(r, g, b, animAlpha));

            float nameScale = 1.1f * guiScale * scaleProgress;
            float maxNameWidth = rw - 14 * guiScale;
            float nameWidth = text.getWidth(module.getName(), nameScale);
            if (nameWidth > maxNameWidth && nameWidth > 0) nameScale *= maxNameWidth / nameWidth;

            float descScale = 0.62f * guiScale * scaleProgress;
            float maxDescWidth = rw - 16 * guiScale;
            float descWidth = text.getWidth(module.getDescription(), descScale);
            if (descWidth > maxDescWidth && descWidth > 0) descScale *= maxDescWidth / descWidth;

            float nameHeight = text.getHeight(nameScale);
            float descHeight = text.getHeight(descScale);
            float blockHeight = nameHeight + 3 * guiScale + descHeight;
            float startY = renderY + (rh - blockHeight) / 2f;

            int textAlpha = (int) (255 * alpha * scaleProgress);
            text.addText(module.getName(), renderX + (rw - (Math.min(nameWidth, maxNameWidth))) / 2f, startY - 0.6f * guiScale, nameScale, new Color(255, 255, 255, textAlpha));
            text.addText(module.getDescription(), renderX + (rw - (Math.min(descWidth, maxDescWidth))) / 2f, startY + nameHeight + 3 * guiScale - 0.2f * guiScale, descScale, new Color(200, 200, 200, textAlpha));
        }
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks) {
        render(set, mouseX, mouseY, deltaTicks, 1.0f);
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float radius = guiScale * 20f;
        BlurRenderer.getInstance().drawBlur(x, y, this.width * guiScale, this.height * guiScale, 0, radius, radius, 0, ClickGui.INSTANCE.blurStrength.getValue().floatValue());

        targetState = (isSettingsActive() && !this.closeSettingsRequested) ? 1 : 0;

        if (currentState != targetState) {
            if (targetState == 1) {
                currentState = 2;
                viewAnimation.setStartValue(0.0f);
            } else {
                currentState = 3;
                viewAnimation.setStartValue(1.0f);
                exitAnimationStarted = false;
            }
        }

        if (currentState == 2) {
            viewAnimation.run(1.0f);
            if (viewAnimation.getValue() >= 0.99f) currentState = 1;
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
            renderSettingsView(set, mouseX, mouseY, deltaTicks, alpha);
        } else if (currentState == 3) {
            closeSettingsRequested = true;
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
            if (settingsComponent != null) {
                if (!exitAnimationStarted) {
                    settingsComponent.startExitAnimation(sourceCardX, sourceCardY, sourceCardW, sourceCardH);
                    exitAnimationStarted = true;
                }
                renderSettingsView(set, mouseX, mouseY, deltaTicks, alpha);
                if (settingsComponent.isAnimationFinished()) {
                    currentState = 0;
                    clearSettingsModule();
                    closeSettingsRequested = false;
                    exitAnimationStarted = false;
                }
            }
        } else if (currentState == 1) {
            renderSettingsView(set, mouseX, mouseY, deltaTicks, alpha);
        } else {
            renderListView(set, mouseX, mouseY, deltaTicks, alpha);
        }
    }

    private static Color applyAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        if (currentState == 2 || currentState == 3) return true;

        if (ColorSettingComponent.hasActivePicker() && ColorSettingComponent.isMouseOutOfPicker((int) event.x(), (int) event.y())) {
            ColorSettingComponent.closeActivePicker();
            return true;
        }

        if (ColorSettingComponent.hasActivePicker() && currentState == 1) {
            boolean handled = settingsViewMouseClicked(event, focused);
            if (consumeSettingsExitRequest()) closeSettingsRequested = true;
            return handled;
        }

        if (!MouseUtils.isHovering(x, y, this.width * guiScale, this.height * guiScale, event.x(), event.y())) {
            listViewClickOutside();
            settingsViewClickOutside();
            return false;
        }

        if (currentState == 1) {
            boolean handled = settingsViewMouseClicked(event, focused);
            if (consumeSettingsExitRequest()) closeSettingsRequested = true;
            return handled;
        }

        if (currentState == 0) {
            boolean handled = listViewMouseClicked(event, focused);
            Module open = consumeRequestedSettingsModule();
            if (open != null) {
                closeSettingsRequested = false;
                setSettingsModuleWithSource(open, sourceCardX, sourceCardY, sourceCardW, sourceCardH);
                if (settingsComponent != null) {
                    float padding = 8 * guiScale;
                    float searchHeight = 24 * guiScale;
                    float targetX = this.x + padding;
                    float targetY = this.y + padding + searchHeight + padding;
                    float targetW = Math.max(0.0f, this.width * guiScale - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
                    settingsComponent.setFilterText("");
                    int itemCount = settingsComponent.getFilteredVisibleCount();
                    float titleH = 18.0f * guiScale;
                    float targetH = 8.0f * guiScale + titleH + 6.0f * guiScale + (itemCount > 0 ? itemCount * 18.0f * guiScale + Math.max(0, itemCount - 1) * 4.0f * guiScale : 0) + 8.0f * guiScale;
                    settingsComponent.initAnimation(sourceCardX, sourceCardY, sourceCardW, sourceCardH, targetX, targetY, targetW, targetH);
                }
                currentState = 2;
                viewAnimation.setStartValue(0.0f);
                return true;
            }
            return handled;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (currentState == 2 || currentState == 3) return true;
        if (ColorSettingComponent.hasActivePicker() && currentState == 1) return settingsViewMouseReleased(event);
        if (currentState == 1) return settingsViewMouseReleased(event);
        if (currentState == 0) return listViewMouseReleased(event);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (currentState == 2 || currentState == 3) return true;
        if (currentState == 1) return settingsViewMouseScrolled(mouseX, mouseY, scrollY);
        if (currentState == 0) return listViewMouseScrolled(mouseX, mouseY, scrollY);
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (currentState == 2 || currentState == 3) return true;
        if (currentState == 1) return settingsViewKeyPressed(event);
        if (currentState == 0) return listViewKeyPressed(event);
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (currentState == 2 || currentState == 3) return true;
        if (currentState == 1) return settingsViewCharTyped(event);
        if (currentState == 0) return listViewCharTyped(event);
        return false;
    }
}