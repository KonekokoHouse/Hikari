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

    private ModuleList() {
        super("ModuleList", Category.RENDER);
    }

    private final DoubleSetting scale = doubleSetting("缩放", 1.0, 0.5, 2.0, 0.1);
    private final ColorSetting shadowColor = colorSetting("阴影颜色", new Color(68, 0, 0, 94));
    private final DoubleSetting glowRadius = doubleSetting("发光半径", 3.0, 1.0, 10.0, 0.5);
    private final DoubleSetting glowIntensity = doubleSetting("发光强度", 1.0, 1.0, 5.0, 1.0);
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

        List<ItemInfo> items = new ArrayList<>();

        for (Module module : enabledModules) {
//            String text = "中文".equals(language.getValue()) ? module.getCnName() : module.getDescription();
            String text = module.getName();
            if (showCategory.getValue()) {
                text += " [" + module.category.getName() + "]";
            }
            float textWidth = textRenderer.getWidth(text, moduleScale);
            float boxWidth = textWidth + 4.0f * moduleScale * 2;
            float boxHeight = 16.0f * moduleScale;
            float totalWidth = boxWidth;
            if (showIcon.getValue()) {
                totalWidth += boxHeight + 2.0f * moduleScale;
            }
            items.add(new ItemInfo(module, text, boxWidth, boxHeight, totalWidth));
        }

        float currentY = 4.0f * moduleScale;

        for (ItemInfo item : items) {
            float totalX = screenWidth - item.totalWidth() - 4.0f * moduleScale;
            float boxY = currentY;

            float textBoxX = totalX;
            float iconBoxX = totalX + item.boxWidth() + 2.0f * moduleScale;

            shadowRenderer.addShadow(textBoxX, boxY, item.boxWidth(), item.boxHeight(), 6.0f * moduleScale, 10.0f * moduleScale, shadowColor.getValue());

            float textX = textBoxX + 4.0f * moduleScale - 1.5f;
            float textY = boxY + (item.boxHeight() - textRenderer.getHeight(moduleScale)) / 5.0f;
//            if ("中文".equals(language.getValue())) {
            textRenderer.addGlowingText(item.text(), textX + 1, textY, moduleScale, new Color(255, 255, 255, 126), glowRadius.getValue().floatValue(), glowIntensity.getValue().intValue());
//            } else {
//                textRenderer.addGlowingText(item.text(), textX + 0.7f, textY - 0.5f, moduleScale, new Color(255, 255, 255, 126), glowRadius.getValue().floatValue(), glowIntensity.getValue().intValue());
//            }

            if (showIcon.getValue()) {
                shadowRenderer.addShadow(iconBoxX, boxY, item.boxHeight(), item.boxHeight(), 6.0f * moduleScale, 10.0f * moduleScale, shadowColor.getValue());

                String iconChar = item.module().category.icon;
                float iconScale = moduleScale * 0.8f;
                float iconWidth = textRenderer.getWidth(iconChar, iconScale, StaticFontLoader.ICONS);
                float iconHeight = textRenderer.getHeight(iconScale, StaticFontLoader.ICONS);
                float iconX = iconBoxX + (item.boxHeight() - iconWidth) / 3.0f;
                float iconY = boxY + (item.boxHeight() - iconHeight) / 5.0f;
                textRenderer.addGlowingText(iconChar, iconX, iconY, iconScale, new Color(255, 255, 255, 92), 3.0f, 1, StaticFontLoader.ICONS);
            }

            currentY += item.boxHeight() + 2.0f * moduleScale;
        }

        shadowRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }

    private int getTextWidth(Module module) {
        TextRenderer textRenderer = textRendererSupplier.get();
        String text = module.getName();
        if (showCategory.getValue()) {
            text += " [" + module.category.getName() + "]";
        }
        return (int) textRenderer.getWidth(text, scale.getValue().floatValue());
    }

    private record ItemInfo(Module module, String text, float boxWidth, float boxHeight, float totalWidth) {
    }
}
