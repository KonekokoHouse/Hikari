package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.IComponent;
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

public final class ListViewController {
    private final Minecraft mc = Minecraft.getInstance();

    private final RoundRectRenderer listRoundRect = new RoundRectRenderer();
    private final TextRenderer listFont = new TextRenderer();
    private final List<ModuleCard> moduleCards = new ArrayList<>();
    private final TranslateComponent searchComponent = TranslateComponent.create("gui", "search");

    private String searchText = "";
    private boolean searchFocused = false;
    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float maxScroll = 0.0f;
    private boolean draggingScrollbar = false;
    private float scrollbarDragStartMouseY = 0.0f;
    private float scrollbarDragStartScroll = 0.0f;

    private float lastSearchBoxX, lastSearchBoxY, lastSearchBoxW, lastSearchBoxH;
    private float lastListX, lastListY, lastListW, lastListH;
    private float lastScrollbarX, lastScrollbarY, lastScrollbarW, lastScrollbarH;
    private float lastThumbY, lastThumbH;
    private float lastIconBoxX, lastIconBoxY, lastIconBoxW, lastIconBoxH;

    private OpenRequest openRequest;

    private static final float CARD_ASPECT_WIDTH = 16.0f;
    private static final float CARD_ASPECT_HEIGHT = 9.0f;

    public void setModules(List<Module> modules) {
        moduleCards.clear();
        for (Module module : modules) {
            moduleCards.add(new ModuleCard(module));
        }
        searchText = "";
        searchFocused = false;
        scrollOffset = 0.0f;
        scrollTarget = 0.0f;
        maxScroll = 0.0f;
        draggingScrollbar = false;
        openRequest = null;
    }

    public OpenRequest consumeOpenRequest() {
        OpenRequest value = openRequest;
        openRequest = null;
        return value;
    }

    public void render(IComponent.RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale, Module suppressModule) {
        float padding = 8 * guiScale;
        float spacing = 4 * guiScale;
        float searchHeight = 24 * guiScale;
        float scaledPanelWidth = panelWidth * guiScale;
        float scaledPanelHeight = panelHeight * guiScale;

        float iconBoxWidth = (scaledPanelWidth - padding * 2 - spacing) * 0.1f;
        float searchBoxWidth = (scaledPanelWidth - padding * 2 - spacing) * 0.9f;

        lastIconBoxX = panelX + padding;
        lastIconBoxY = panelY + padding;
        lastIconBoxW = iconBoxWidth;
        lastIconBoxH = searchHeight;

        renderIconBox(set, lastIconBoxX, lastIconBoxY, iconBoxWidth, searchHeight, guiScale, MouseUtils.isHovering(lastIconBoxX, lastIconBoxY, iconBoxWidth, searchHeight, mouseX, mouseY), alpha);

        lastSearchBoxX = lastIconBoxX + iconBoxWidth + spacing;
        lastSearchBoxY = lastIconBoxY;
        lastSearchBoxW = searchBoxWidth;
        lastSearchBoxH = searchHeight;
        renderSearchBox(set, lastSearchBoxX, lastSearchBoxY, searchBoxWidth, searchHeight, guiScale, searchFocused, MouseUtils.isHovering(lastSearchBoxX, lastSearchBoxY, searchBoxWidth, searchHeight, mouseX, mouseY), searchText, alpha);

        lastListX = panelX + padding;
        lastListY = lastIconBoxY + searchHeight + padding;
        lastListW = Math.max(0.0f, scaledPanelWidth - padding * 2 - 4.0f * guiScale - 4.0f * guiScale);
        lastListH = Math.max(0.0f, panelY + scaledPanelHeight - padding - lastListY);

        lastScrollbarX = lastListX + lastListW + 4.0f * guiScale;
        lastScrollbarY = lastListY;
        lastScrollbarW = 4.0f * guiScale;
        lastScrollbarH = lastListH;

        List<ModuleCard> visibleCards = new ArrayList<>();
        for (ModuleCard card : moduleCards) {
            boolean matchesSearch = searchText.isEmpty() || card.module.getTranslatedName().toLowerCase().startsWith(searchText.toLowerCase());
            card.updateVisibility(matchesSearch);
            if (!matchesSearch && card.scaleAnimation.getValue() <= 0.01f) {
                card.width = 0;
                card.height = 0;
                continue;
            }
            visibleCards.add(card);
        }

        if (visibleCards.isEmpty() || lastListH <= 0.0f || lastListW <= 0.0f) {
            maxScroll = 0.0f;
            scrollOffset = 0.0f;
            scrollTarget = 0.0f;
            draggingScrollbar = false;
            lastThumbY = 0.0f;
            lastThumbH = 0.0f;
            return;
        }

        float itemGap = 8 * guiScale;
        int columns = Math.max(3, Mth.floor((lastListW + itemGap) / (120 * guiScale + itemGap)));
        float cardWidth = (lastListW - itemGap * (columns - 1)) / columns;
        float cardHeight = cardWidth * (CARD_ASPECT_HEIGHT / CARD_ASPECT_WIDTH);
        int totalRows = Mth.ceil(visibleCards.size() / (double) columns);
        float contentH = totalRows * cardHeight + Math.max(0, totalRows - 1) * itemGap;

        maxScroll = Math.max(0.0f, contentH - lastListH);
        scrollTarget = Mth.clamp(scrollTarget, 0.0f, maxScroll);
        scrollOffset += (scrollTarget - scrollOffset) * 0.35f;
        scrollOffset = Mth.clamp(scrollOffset, 0.0f, maxScroll);

        lastThumbH = maxScroll <= 0.0f ? lastListH : Math.max(12.0f * guiScale, lastListH * (lastListH / contentH));
        float thumbTravel = Math.max(0.0f, lastListH - lastThumbH);
        lastThumbY = maxScroll <= 0.0f ? lastListY : lastListY + (scrollOffset / maxScroll) * thumbTravel;

        if (draggingScrollbar && maxScroll > 0.0f) {
            scrollTarget = handleScrollDrag(scrollTarget, maxScroll, lastThumbH, lastScrollbarH, mouseY, scrollbarDragStartMouseY, scrollbarDragStartScroll);
        }

        float pxScale = (float) mc.getWindow().getGuiScale();
        int scX = Mth.clamp(Mth.floor((lastListX - padding) * pxScale), 0, mc.getWindow().getWidth());
        int scY = Mth.clamp(Mth.floor((mc.getWindow().getGuiScaledHeight() - (lastListY + lastListH)) * pxScale), 0, mc.getWindow().getHeight());
        int scW = Mth.clamp(Mth.ceil((lastListW + padding * 2) * pxScale), 0, mc.getWindow().getWidth() - scX);
        int scH = Mth.clamp(Mth.ceil(lastListH * pxScale), 0, mc.getWindow().getHeight() - scY);
        listRoundRect.setScissor(scX, scY, scW, scH);
        listFont.setScissor(scX, scY, scW, scH);

        float listBottom = panelY + scaledPanelHeight - padding;
        int visibleIndex = 0;
        for (ModuleCard card : visibleCards) {
            int row = visibleIndex / columns;
            int col = visibleIndex % columns;
            card.x = lastListX + col * (cardWidth + itemGap);
            card.y = lastListY + row * (cardHeight + itemGap) - scrollOffset;
            card.width = cardWidth;
            card.height = cardHeight;
            if (card.shouldRender() && card.y + cardHeight >= lastListY && card.y <= listBottom) {
                if (suppressModule == null || suppressModule != card.module) {
                    card.render(listRoundRect, listFont, mouseX, mouseY, guiScale, alpha);
                }
            }
            visibleIndex++;
        }

        listRoundRect.drawAndClear();
        listFont.drawAndClear();
        listRoundRect.clearScissor();
        listFont.clearScissor();

        if (maxScroll > 0.0f) {
            renderScrollbar(set, lastScrollbarX, lastListY, lastScrollbarW, lastListH, lastThumbY, lastThumbH, draggingScrollbar, MouseUtils.isHovering(lastScrollbarX, lastListY, lastScrollbarW, lastListH, mouseX, mouseY), MouseUtils.isHovering(lastScrollbarX, lastThumbY, lastScrollbarW, lastThumbH, mouseX, mouseY), alpha);
        }
    }

    boolean mouseClicked(MouseButtonEvent event, boolean focused, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        float scaledWidth = panelWidth * guiScale;
        float scaledHeight = panelHeight * guiScale;
        if (!MouseUtils.isHovering(panelX, panelY, scaledWidth, scaledHeight, event.x(), event.y())) return false;

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
                    openRequest = new OpenRequest(card.module, card.getRenderX(), card.getRenderY(), card.getRenderW(), card.getRenderH());
                    draggingScrollbar = false;
                    return true;
                }
            }
        }

        return true;
    }

    boolean mouseReleased(MouseButtonEvent event, float panelX, float panelY, float panelWidth, float panelHeight, float guiScale) {
        draggingScrollbar = false;
        return MouseUtils.isHovering(panelX, panelY, panelWidth * guiScale, panelHeight * guiScale, event.x(), event.y());
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        scrollTarget = handleMouseScroll(scrollTarget, maxScroll, lastListX, lastListY, lastListW, lastListH, lastScrollbarW, mouseX, mouseY, scrollY);
        return maxScroll > 0.0f && MouseUtils.isHovering(lastListX, lastListY, lastListW + lastScrollbarW, lastListH, mouseX, mouseY);
    }

    boolean keyPressed(KeyEvent event) {
        if (!searchFocused) return false;
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                scrollTarget = 0.0f;
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
            searchFocused = false;
            return true;
        }
        return false;
    }

    boolean charTyped(CharacterEvent event) {
        if (!searchFocused) return false;
        searchText += Character.toString(event.codepoint());
        scrollTarget = 0.0f;
        return true;
    }

    void clickOutside() {
        searchFocused = false;
        draggingScrollbar = false;
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

    public record OpenRequest(Module module, float sourceX, float sourceY, float sourceW, float sourceH) {
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
            float scaleProgress = Mth.clamp(scaleAnimation.getValue(), 0.0f, 1.0f);
            float baseScale = 0.5f + 0.5f * scaleProgress;
            float rw = width * baseScale;
            float centerX = x + width / 2.0f;
            return centerX - rw / 2.0f;
        }

        private float getRenderY() {
            float scaleProgress = Mth.clamp(scaleAnimation.getValue(), 0.0f, 1.0f);
            float baseScale = 0.5f + 0.5f * scaleProgress;
            float rh = height * baseScale;
            float centerY = y + height / 2.0f;
            return centerY - rh / 2.0f;
        }

        private float getRenderW() {
            float scaleProgress = Mth.clamp(scaleAnimation.getValue(), 0.0f, 1.0f);
            float baseScale = 0.5f + 0.5f * scaleProgress;
            return width * baseScale;
        }

        private float getRenderH() {
            float scaleProgress = Mth.clamp(scaleAnimation.getValue(), 0.0f, 1.0f);
            float baseScale = 0.5f + 0.5f * scaleProgress;
            return height * baseScale;
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
            Color onColor = new Color(0x35FFFFFF, true);
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
            float nameWidth = text.getWidth(module.getTranslatedName(), nameScale);
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
            text.addText(module.getTranslatedName(), renderX + (rw - (Math.min(nameWidth, maxNameWidth))) / 2f, startY - 0.6f * guiScale, nameScale, new Color(255, 255, 255, textAlpha));
            text.addText(module.getDescription(), renderX + (rw - (Math.min(descWidth, maxDescWidth))) / 2f, startY + nameHeight + 3 * guiScale - 0.2f * guiScale, descScale, new Color(200, 200, 200, textAlpha));
        }
    }
}
