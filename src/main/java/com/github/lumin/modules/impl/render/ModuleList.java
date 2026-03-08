package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.managers.Managers;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.google.common.base.Suppliers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModuleList extends Module {

    public static final ModuleList INSTANCE = new ModuleList();

    public ModuleList() {
        super("功能列表", "ModuleList", Category.RENDER);
    }

    private final DoubleSetting scale = doubleSetting("缩放", 1.0, 0.5, 2.0, 0.1);
    private final ColorSetting shadowColor = colorSetting("阴影颜色", new Color(68, 0, 0, 94));
    private final BoolSetting showCategory = boolSetting("显示分类", false);
    private final BoolSetting showIcon = boolSetting("显示图标", true);

    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);
    private final Supplier<ShadowRenderer> shadowRendererSupplier = Suppliers.memoize(ShadowRenderer::new);

    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {
        if (nullCheck()) return;

        List<Module> enabledModules = Managers.MODULE.getModules().stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());

        if (enabledModules.isEmpty()) return;

        enabledModules.sort(Comparator.comparingInt(m -> -getTextWidth(m)));

        TextRenderer textRenderer = textRendererSupplier.get();
        ShadowRenderer shadowRenderer = shadowRendererSupplier.get();

        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float moduleScale = scale.getValue().floatValue();

        float padding = 4.0f * moduleScale;
        float itemHeight = 16.0f * moduleScale;
        float gap = 2.0f * moduleScale;
        float iconGap = 2.0f * moduleScale;
        float iconBoxSize = itemHeight;

        List<ItemInfo> items = new ArrayList<>();

        for (Module module : enabledModules) {
            String text = showCategory.getValue()
                    ? module.getChineseName() + " [" + module.category.getName() + "]"
                    : module.getChineseName();
            float textWidth = textRenderer.getWidth(text, moduleScale);
            float boxWidth = textWidth + padding * 2;
            float boxHeight = itemHeight;
            float totalWidth = boxWidth;
            if (showIcon.getValue()) {
                totalWidth += iconBoxSize + iconGap;
            }
            items.add(new ItemInfo(module, text, boxWidth, boxHeight, totalWidth));
        }

        float currentY = getPositionY();

        for (ItemInfo item : items) {
            float totalX = getPositionX(screenWidth, item.totalWidth());
            float boxY = currentY;

            float textBoxX;
            float iconBoxX;

            textBoxX = totalX;
            iconBoxX = totalX + item.boxWidth() + iconGap;

            shadowRenderer.addShadow(textBoxX, boxY, item.boxWidth(), item.boxHeight(), 6.0f * moduleScale, 10.0f * moduleScale, shadowColor.getValue());

            float textX = textBoxX + padding - 1.5f;
            float textY = boxY + (item.boxHeight() - textRenderer.getHeight(moduleScale)) / 5.0f;
            textRenderer.addText(item.text(), textX, textY, moduleScale, new Color(0xFFFFFF));

            if (showIcon.getValue()) {
                shadowRenderer.addShadow(iconBoxX, boxY, iconBoxSize, iconBoxSize, 6.0f * moduleScale, 10.0f * moduleScale, shadowColor.getValue());

                String iconChar = item.module().category.icon;
                float iconScale = moduleScale * 0.8f;
                float iconWidth = textRenderer.getWidth(iconChar, iconScale, StaticFontLoader.ICONS);
                float iconHeight = textRenderer.getHeight(iconScale, StaticFontLoader.ICONS);
                float iconX = iconBoxX + (iconBoxSize - iconWidth) / 3.0f;
                float iconY = boxY + (iconBoxSize - iconHeight) / 5.0f;
                textRenderer.addText(iconChar, iconX, iconY, iconScale, new Color(0xFFFFFF), StaticFontLoader.ICONS);
            }

            currentY += item.boxHeight() + gap;
        }

        shadowRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private int getTextWidth(Module module) {
        TextRenderer textRenderer = textRendererSupplier.get();
        String text = showCategory.getValue()
                ? module.getChineseName() + " [" + module.category.getName() + "]"
                : module.getChineseName();
        return (int) textRenderer.getWidth(text, scale.getValue().floatValue());
    }

    private float getPositionX(float screenWidth, float boxWidth) {
        float margin = 4.0f * scale.getValue().floatValue();
        return screenWidth - boxWidth - margin;
    }

    private float getPositionY() {
        return 4.0f * scale.getValue().floatValue();
    }

    private record ItemInfo(Module module, String text, float boxWidth, float boxHeight, float totalWidth) {
    }
}
