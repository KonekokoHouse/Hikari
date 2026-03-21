package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.managers.ModuleManager;
import com.github.lumin.modules.Category;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class CategoryRailPanel {

    protected final DropdownState state;
    private final RectRenderer rectRenderer;
    private final RoundRectRenderer roundRectRenderer;
    private final TextRenderer textRenderer;
    private final TextRenderer clippedTextRenderer = new TextRenderer();
    private final Animation expandAnimation = new Animation(Easing.EASE_OUT_CUBIC, 240L);
    private final Animation contentAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation menuHoverAnimation = new Animation(Easing.EASE_OUT_CUBIC, 120L);
    private final Animation headerTitleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220L);
    private final Animation headerSubtitleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 260L);
    private final Animation headerDividerAnimation = new Animation(Easing.EASE_OUT_CUBIC, 220L);
    private final Animation selectionYAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private final Animation selectionHeightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180L);
    private DropdownLayout.Rect bounds;
    private boolean clippedTextPending;

    public CategoryRailPanel(DropdownState state, RectRenderer rectRenderer, RoundRectRenderer roundRectRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.rectRenderer = rectRenderer;
        this.roundRectRenderer = roundRectRenderer;
        this.textRenderer = textRenderer;
        this.expandAnimation.setStartValue(DropdownTheme.RAIL_COLLAPSED_WIDTH);
        this.contentAnimation.setStartValue(0.0f);
        this.menuHoverAnimation.setStartValue(0.0f);
        this.headerTitleAnimation.setStartValue(0.0f);
        this.headerSubtitleAnimation.setStartValue(0.0f);
        this.headerDividerAnimation.setStartValue(0.0f);
        this.selectionYAnimation.setStartValue(0.0f);
        this.selectionHeightAnimation.setStartValue(32.0f);
    }

    public float getAnimatedWidth() {
        expandAnimation.run(state.isSidebarExpanded() ? DropdownTheme.RAIL_EXPANDED_WIDTH : DropdownTheme.RAIL_COLLAPSED_WIDTH);
        return expandAnimation.getValue();
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        applyTextScissor(bounds, guiGraphics.guiHeight());
        float progress = Math.max(0.0f, Math.min(1.0f, (getAnimatedWidth() - DropdownTheme.RAIL_COLLAPSED_WIDTH) / (DropdownTheme.RAIL_EXPANDED_WIDTH - DropdownTheme.RAIL_COLLAPSED_WIDTH)));
        contentAnimation.run(state.isSidebarExpanded() ? 1.0f : 0.0f);
        float contentProgress = contentAnimation.getValue();
        headerTitleAnimation.run(contentProgress);
        headerSubtitleAnimation.run(contentProgress > 0.08f ? 1.0f : 0.0f);
        headerDividerAnimation.run(contentProgress > 0.12f ? 1.0f : 0.0f);
        float titleScale = 0.78f;
        float subtitleScale = 0.52f;
        float itemIconScale = 1.02f;
        float itemLabelScale = 0.62f;
        float itemCountScale = 0.58f;

        DropdownLayout.Rect menuButton = getMenuButtonBounds();
        menuHoverAnimation.run(mouseOver(menuButton, mouseX, mouseY) ? 1.0f : 0.0f);
        Color buttonColor = DropdownTheme.lerp(DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, 0), DropdownTheme.SURFACE_CONTAINER_HIGH, menuHoverAnimation.getValue());
        roundRectRenderer.addRoundRect(menuButton.x(), menuButton.y(), menuButton.width(), menuButton.height(), 12.0f, buttonColor);
        drawMenuGlyph(menuButton);

        float titleProgress = headerTitleAnimation.getValue();
        float subtitleProgress = headerSubtitleAnimation.getValue();
        float dividerProgress = headerDividerAnimation.getValue();
        if (titleProgress > 0.02f) {
            Color brandColor = DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, (int) (255 * titleProgress));
            Color subColor = DropdownTheme.withAlpha(DropdownTheme.TEXT_SECONDARY, (int) (210 * subtitleProgress));
            float titleY = bounds.y() + 8.0f;
            float titleHeight = clippedTextRenderer.getHeight(titleScale, StaticFontLoader.DUCKSANS);
            float pad = 2.0f;
            float subtitleY = titleY + titleHeight + pad;
            float titleOffset = (1.0f - titleProgress) * 8.0f;
            float subtitleOffset = (1.0f - subtitleProgress) * 10.0f;
            clippedTextRenderer.addText("Epsilon 5.0", bounds.x() + 38.0f + titleOffset, titleY, titleScale, brandColor, StaticFontLoader.DUCKSANS);
            if (subtitleProgress > 0.02f) {
                clippedTextRenderer.addText("1.21.11 Rewrite", bounds.x() + 38.0f + subtitleOffset, subtitleY, subtitleScale, subColor);
            }
            if (dividerProgress > 0.02f) {
                float dividerY = bounds.y() + 35.0f;
                float dividerBaseX = bounds.x() + 7.0f;
                float dividerTargetWidth = bounds.width() - 14.0f;
                float dividerWidth = dividerTargetWidth * dividerProgress;
                float dividerX = dividerBaseX + (1.0f - dividerProgress) * 6.0f;
                int dividerAlpha = (int) (120 * dividerProgress);
                rectRenderer.addRect(dividerX, dividerY, dividerWidth, 1.0f, DropdownTheme.withAlpha(DropdownTheme.OUTLINE_SOFT, dividerAlpha));
                rectRenderer.addRect(dividerX, dividerY, Math.min(18.0f, dividerWidth), 1.0f, DropdownTheme.withAlpha(DropdownTheme.TEXT_SECONDARY, (int) (52 * dividerProgress)));
            }
        }

        float selectedItemY = bounds.y() + 40.0f;
        float selectedItemHeight = progress > 0.35f ? 34.0f : 32.0f;
        float lookupY = bounds.y() + 40.0f;
        for (Category category : Category.values()) {
            if (state.getSelectedCategory() == category) {
                selectedItemY = lookupY;
                break;
            }
            lookupY += progress > 0.35f ? 38.0f : 36.0f;
        }

        selectionYAnimation.run(selectedItemY);
        selectionHeightAnimation.run(selectedItemHeight);
        float animatedSelectionY = selectionYAnimation.getValue();
        float animatedSelectionHeight = selectionHeightAnimation.getValue();
        roundRectRenderer.addRoundRect(bounds.x() + 5.0f, animatedSelectionY, bounds.width() - 10.0f, animatedSelectionHeight, DropdownTheme.CARD_RADIUS, DropdownTheme.SECONDARY_CONTAINER);

        float itemY = bounds.y() + 40.0f;
        for (Category category : Category.values()) {
            float itemHeight = progress > 0.35f ? 34.0f : 32.0f;
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, itemHeight);
            boolean hovered = itemRect.contains(mouseX, mouseY);
            boolean selected = state.getSelectedCategory() == category;

            Color background = selected ? DropdownTheme.withAlpha(DropdownTheme.SECONDARY_CONTAINER, 0) : hovered ? DropdownTheme.SURFACE_CONTAINER : DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER, 0);
            Color iconColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : hovered ? DropdownTheme.TEXT_PRIMARY : DropdownTheme.TEXT_SECONDARY;
            Color labelColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : DropdownTheme.TEXT_PRIMARY;
            Color countColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : DropdownTheme.TEXT_SECONDARY;
            float iconHeight = clippedTextRenderer.getHeight(itemIconScale, StaticFontLoader.ICONS);
            float labelHeight = clippedTextRenderer.getHeight(itemLabelScale, StaticFontLoader.DUCKSANS);
            float countHeight = clippedTextRenderer.getHeight(itemCountScale);
            float iconY = itemRect.y() + (itemRect.height() - iconHeight) / 2.0f - 1.0f;
            float labelY = itemRect.y() + (itemRect.height() - labelHeight) / 2.0f - 1.0f;
            float countY = itemRect.y() + (itemRect.height() - countHeight) / 2.0f - 1.0f;

            roundRectRenderer.addRoundRect(itemRect.x(), itemRect.y(), itemRect.width(), itemRect.height(), DropdownTheme.CARD_RADIUS, background);
            float iconWidth = clippedTextRenderer.getWidth(category.icon, itemIconScale, StaticFontLoader.ICONS);
            float iconX = menuButton.x() + (menuButton.width() - iconWidth) / 2.0f;
            clippedTextRenderer.addText(category.icon, iconX, iconY, itemIconScale, iconColor, StaticFontLoader.ICONS);

            if (contentProgress > 0.02f) {
                int count = getCategoryCount(category);
                float textOffset = (1.0f - contentProgress) * 5.0f;
                Color animatedLabel = DropdownTheme.withAlpha(labelColor, (int) (255 * contentProgress));
                Color animatedCount = DropdownTheme.withAlpha(countColor, (int) (220 * contentProgress));
                clippedTextRenderer.addText(category.getName(), itemRect.x() + 30.0f + textOffset, labelY, itemLabelScale, animatedLabel, StaticFontLoader.DUCKSANS);
                float countWidth = clippedTextRenderer.getWidth(Integer.toString(count), itemCountScale);
                clippedTextRenderer.addText(Integer.toString(count), itemRect.right() - 12.0f - countWidth, countY, itemCountScale, animatedCount);
            }

            itemY += progress > 0.35f ? 38.0f : 36.0f;
        }

        clippedTextPending = true;
    }

    public void flushClippedText() {
        clippedTextRenderer.drawAndClear();
        if (clippedTextPending) {
            clearTextScissor();
            clippedTextPending = false;
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        if (getMenuButtonBounds().contains(event.x(), event.y())) {
            state.toggleSidebarExpanded();
            return true;
        }

        float progress = Math.max(0.0f, Math.min(1.0f, (getAnimatedWidth() - DropdownTheme.RAIL_COLLAPSED_WIDTH) / (DropdownTheme.RAIL_EXPANDED_WIDTH - DropdownTheme.RAIL_COLLAPSED_WIDTH)));
        float itemY = bounds.y() + 40.0f;
        for (Category category : Category.values()) {
            DropdownLayout.Rect itemRect = new DropdownLayout.Rect(bounds.x() + 5.0f, itemY, bounds.width() - 10.0f, progress > 0.35f ? 34.0f : 32.0f);
            if (itemRect.contains(event.x(), event.y())) {
                state.setSelectedCategory(category);
                return true;
            }
            itemY += progress > 0.35f ? 38.0f : 36.0f;
        }
        return false;
    }

    private DropdownLayout.Rect getMenuButtonBounds() {
        return new DropdownLayout.Rect(bounds.x() + 4.0f, bounds.y() + 4.0f, 28.0f, 28.0f);
    }

    private boolean mouseOver(DropdownLayout.Rect rect, int mouseX, int mouseY) {
        return rect.contains(mouseX, mouseY);
    }

    private void drawMenuGlyph(DropdownLayout.Rect button) {
        Color lineColor = DropdownTheme.TEXT_PRIMARY;
        float glyphWidth = 12.0f;
        float glyphHeight = 10.0f;
        float x = button.x() + (button.width() - glyphWidth) / 2.0f;
        float y = button.y() + (button.height() - glyphHeight) / 2.0f;
        rectRenderer.addRect(x, y, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 4.0f, 12.0f, 1.6f, lineColor);
        rectRenderer.addRect(x, y + 8.0f, 12.0f, 1.6f, lineColor);
    }

    private int getCategoryCount(Category category) {
        return (int) ModuleManager.INSTANCE.getModules().stream().filter(module -> module.category == category).count();
    }

    private void applyTextScissor(DropdownLayout.Rect rect, int guiHeight) {
        int scale = Minecraft.getInstance().getWindow().getGuiScale();
        int x = Math.round(rect.x() * scale);
        int y = Math.round((guiHeight - rect.bottom()) * scale);
        int width = Math.round(rect.width() * scale);
        int height = Math.round(rect.height() * scale);
        clippedTextRenderer.setScissor(x, y, width, height);
    }

    private void clearTextScissor() {
        clippedTextRenderer.clearScissor();
    }

}
