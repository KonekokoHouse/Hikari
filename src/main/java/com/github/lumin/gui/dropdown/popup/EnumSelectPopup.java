package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class EnumSelectPopup implements DropdownPopupHost.Popup {

    private final DropdownLayout.Rect bounds;
    private final EnumSetting<?> setting;
    private final DropdownLayout.Rect anchorBounds;
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_CUBIC, 140L);
    private int hoveredIndex = -1;

    public EnumSelectPopup(DropdownLayout.Rect bounds, DropdownLayout.Rect anchorBounds, EnumSetting<?> setting) {
        this.bounds = bounds;
        this.anchorBounds = anchorBounds;
        this.setting = setting;
        this.openAnimation.setStartValue(0.0f);
    }

    public EnumSetting<?> getSetting() {
        return setting;
    }

    @Override
    public DropdownLayout.Rect getBounds() {
        return bounds;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        openAnimation.run(1.0f);
        float progress = openAnimation.getValue();
        float popupY = bounds.y() - (1.0f - progress) * 6.0f;
        int alpha = (int) (245 * progress);

        shadowRenderer.addShadow(bounds.x(), popupY, bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, 14.0f, DropdownTheme.withAlpha(DropdownTheme.SHADOW, (int) (120 * progress)));
        roundRectRenderer.addRoundRect(bounds.x(), popupY, bounds.width(), bounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER_LOW, alpha));
        roundRectRenderer.addRoundRect(anchorBounds.x(), anchorBounds.y(), anchorBounds.width(), anchorBounds.height(), DropdownTheme.CARD_RADIUS, DropdownTheme.withAlpha(DropdownTheme.SECONDARY_CONTAINER, (int) (130 * progress)));

        Enum<?>[] modes = setting.getModes();
        float itemY = popupY + 6.0f;
        hoveredIndex = -1;
        for (int i = 0; i < modes.length; i++) {
            DropdownLayout.Rect itemBounds = new DropdownLayout.Rect(bounds.x() + 6.0f, itemY, bounds.width() - 12.0f, 22.0f);
            boolean hovered = itemBounds.contains(mouseX, mouseY);
            if (hovered) {
                hoveredIndex = i;
            }
            boolean selected = i == setting.getModeIndex();
            Color baseBackground = DropdownTheme.withAlpha(DropdownTheme.SURFACE_CONTAINER_HIGHEST, 0);
            Color hoverBackground = DropdownTheme.lerp(DropdownTheme.SURFACE_CONTAINER_HIGH, DropdownTheme.SURFACE_CONTAINER_HIGHEST, 0.55f);
            Color selectedBackground = DropdownTheme.SECONDARY_CONTAINER;
            Color background = selected ? selectedBackground : hovered ? hoverBackground : baseBackground;
            Color textColor = selected ? DropdownTheme.ON_SECONDARY_CONTAINER : hovered ? DropdownTheme.withAlpha(DropdownTheme.TEXT_PRIMARY, 255) : DropdownTheme.TEXT_SECONDARY;
            roundRectRenderer.addRoundRect(itemBounds.x(), itemY, itemBounds.width(), itemBounds.height(), 8.0f, DropdownTheme.withAlpha(background, alpha));
            if (selected) {
                textRenderer.addText("V", itemBounds.x() + 8.0f, itemY + 6.5f, 0.72f, DropdownTheme.withAlpha(DropdownTheme.ON_SECONDARY_CONTAINER, alpha), StaticFontLoader.ICONS);
            }
            textRenderer.addText(setting.getTranslatedValueByIndex(i), itemBounds.x() + (selected ? 22.0f : 10.0f), itemY + 7.0f, 0.62f, DropdownTheme.withAlpha(textColor, alpha), StaticFontLoader.DUCKSANS);
            itemY += 24.0f;
        }

        shadowRenderer.drawAndClear();
        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!bounds.contains(event.x(), event.y()) || event.button() != 0) {
            return false;
        }
        Enum[] modes = setting.getModes();
        if (hoveredIndex < 0 || hoveredIndex >= modes.length) {
            return false;
        }
        ((EnumSetting) setting).setMode(modes[hoveredIndex]);
        return true;
    }

    @Override
    public boolean shouldCloseAfterClick() {
        return true;
    }

}
