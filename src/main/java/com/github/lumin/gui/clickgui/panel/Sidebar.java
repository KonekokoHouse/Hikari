package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.graphics.shaders.BlurShader;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.IComponent;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Sidebar implements IComponent {

    private final Minecraft mc = Minecraft.getInstance();
    private final List<CategoryBar> categoryBars = new ArrayList<>();
    private Category selectedCategory = Category.values()[0];
    private Consumer<Category> onSelect;
    private final Animation selectedHighlightY = new Animation(Easing.EASE_OUT_QUAD, 160L);
    private boolean highlightInitialized;

    private final TranslateComponent gameAccountText = TranslateComponent.create("gui", "gameaccount");

    public Sidebar() {
        for (Category category : Category.values()) {
            categoryBars.add(new CategoryBar(category));
        }
    }

    public void setOnSelect(Consumer<Category> onSelect) {
        this.onSelect = onSelect;
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    private float x, y, width, height;

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks) {
        render(set, mouseX, mouseY, deltaTicks, 1.0f);
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {

        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float radius = guiScale * 20f;

        float width = this.width * guiScale;
        float height = this.height * guiScale;

        if (ClickGui.INSTANCE.isSidebarBlur()) {
            BlurShader.INSTANCE.drawBlur(x, y, width, height, radius, 0, 0, radius, ClickGui.INSTANCE.getBlurStrength());
        }

        set.bottomRoundRect().addRoundRect(x, y, width, height, radius, 0, 0, radius, applyAlpha(new Color(0x5F000000, true), alpha));

        var player = mc.player;
        String playerName = null;
        Identifier skin = null;
        if (player != null) {
            playerName = player.getName().getString();
            skin = player.getSkin().body().texturePath();
        }

        float padding = 12 * guiScale;
        float headSize = 32 * guiScale;
        float headX = x + padding;
        float headY = y + padding;

        // Outline
        float outline = 0.5f * guiScale;
        set.bottomRoundRect().addRoundRect(headX - outline, headY - outline, headSize + outline * 2, headSize + outline * 2, radius - 12 + outline, applyAlpha(Color.WHITE, alpha));

        if (skin != null) {
            set.texture().addPlayerHead(skin, headX, headY, headSize, radius - 12f, applyAlpha(Color.WHITE, alpha));
        }

        float textX = headX + headSize + 6 * guiScale;
        float nameY = headY + 2 * guiScale;
        float accountY = nameY + 20 * guiScale;

        if (playerName != null) {
            float maxNameWidth = width - (textX - x) - padding;
            float defaultScale = guiScale * 1.5f;
            float minScale = guiScale * 1.0f;
            float currentScale = defaultScale;

            float nameWidth = set.font().getWidth(playerName, currentScale);

            if (nameWidth > maxNameWidth) {
                float scaled = defaultScale * (maxNameWidth / nameWidth);
                if (scaled >= minScale) {
                    currentScale = scaled;
                    set.font().addText(playerName, textX, nameY, currentScale, applyAlpha(Color.WHITE, alpha));
                } else {
                    currentScale = minScale;
                    nameY -= 6 * guiScale;
                    StringBuilder line1 = new StringBuilder();
                    StringBuilder line2 = new StringBuilder();
                    float currentW = 0;
                    boolean wrapped = false;
                    for (char c : playerName.toCharArray()) {
                        float cw = set.font().getWidth(String.valueOf(c), currentScale);
                        if (!wrapped && currentW + cw <= maxNameWidth) {
                            line1.append(c);
                            currentW += cw;
                        } else {
                            wrapped = true;
                            line2.append(c);
                        }
                    }
                    float lineHeight = set.font().getHeight(currentScale);
                    set.font().addText(line1.toString(), textX, nameY, currentScale, applyAlpha(Color.WHITE, alpha));
                    set.font().addText(line2.toString(), textX, nameY + lineHeight, currentScale, applyAlpha(Color.WHITE, alpha));
                }
            } else {
                set.font().addText(playerName, textX, nameY, currentScale, applyAlpha(Color.WHITE, alpha));
            }

            set.font().addText(gameAccountText.getTranslatedName(), textX, accountY + 2, guiScale * 0.7f, applyAlpha(Color.GRAY, alpha));
        }

        // Category Placeholder
        float categoryY = headY + headSize + padding;

        float itemHeight = 24 * guiScale;
        float itemPadding = 4 * guiScale;
        float categoryHeight = categoryBars.size() * (itemHeight + itemPadding) + itemPadding;

        if (categoryHeight > 0) {
            set.bottomRoundRect().addRoundRect(headX, categoryY, width - padding * 2, categoryHeight, radius - 12, applyAlpha(new Color(35, 35, 35, 180), alpha));

            float currentY = categoryY + itemPadding;
            float itemWidth = width - padding * 2 - itemPadding * 2;
            float itemX = headX + itemPadding;

            float selectedTargetY = currentY;
            for (CategoryBar bar : categoryBars) {
                bar.x = itemX;
                bar.y = currentY;
                bar.width = itemWidth;
                bar.height = itemHeight;
                if (bar.category == selectedCategory) {
                    selectedTargetY = currentY;
                }
                currentY += itemHeight + itemPadding;
            }

            if (!highlightInitialized) {
                selectedHighlightY.setStartValue(selectedTargetY);
                highlightInitialized = true;
            }
            selectedHighlightY.run(selectedTargetY);
            float hy = selectedHighlightY.getValue();

            set.bottomRoundRect().addRoundRect(itemX, hy, itemWidth, itemHeight, 8.0f * guiScale, applyAlpha(new Color(255, 255, 255, 52), alpha));

            for (CategoryBar bar : categoryBars) {
                bar.render(set, mouseX, mouseY, guiScale, alpha);
            }
        }
    }

    private Color applyAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * alpha));
    }

    private class CategoryBar {

        private final Category category;
        private float x, y, width, height;

        private CategoryBar(Category category) {
            this.category = category;
        }

        private void render(RendererSet set, int mouseX, int mouseY, float guiScale, float alpha) {

            boolean hovered = MouseUtils.isHovering(x, y, width, height, mouseX, mouseY);
            boolean isSelected = category == selectedCategory;

            if (hovered && !isSelected) {
                set.bottomRoundRect().addRoundRect(x, y, width, height, 8 * guiScale, applyAlpha(new Color(255, 255, 255, 30), alpha));
            }

            float iconScale = guiScale * 1.0f;
            float iconWidth = set.font().getWidth(category.icon, iconScale, StaticFontLoader.ICONS);
            float iconHeight = set.font().getHeight(iconScale, StaticFontLoader.ICONS);

            float iconX = x + 8 * guiScale;
            float iconY = y + (height - iconHeight) / 2f - guiScale;

            set.font().addText(category.icon, iconX, iconY, iconScale, isSelected || hovered ? applyAlpha(Color.WHITE, alpha) : applyAlpha(Color.GRAY, alpha), StaticFontLoader.ICONS);

            float textX = iconX + iconWidth + 6 * guiScale;
            float nameY = y + guiScale;
            float descriptionY = nameY + 12 * guiScale;

            set.font().addText(category.getName(), textX, nameY, guiScale * 0.9f, applyAlpha(Color.WHITE, alpha));
            set.font().addText(category.description, textX, descriptionY, guiScale * 0.6f, applyAlpha(Color.GRAY, alpha));
        }

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float scaledWidth = width * guiScale;
        float scaledHeight = height * guiScale;
        if (!MouseUtils.isHovering(x, y, scaledWidth, scaledHeight, event.x(), event.y())) return false;

        for (CategoryBar bar : categoryBars) {
            if (MouseUtils.isHovering(bar.x, bar.y, bar.width, bar.height, event.x(), event.y())) {
                if (selectedCategory != bar.category) {
                    selectedCategory = bar.category;
                    if (onSelect != null) {
                        onSelect.accept(selectedCategory);
                    }
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float scaledWidth = width * guiScale;
        float scaledHeight = height * guiScale;
        return MouseUtils.isHovering(x, y, scaledWidth, scaledHeight, event.x(), event.y());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return false;
    }

}