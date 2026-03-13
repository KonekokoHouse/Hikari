package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.DoubleSetting;
import com.google.common.base.Suppliers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.awt.*;
import java.util.function.Supplier;

public class HUD extends Module {

    public static final HUD INSTANCE = new HUD();

    private HUD() {
        super("HUD", Category.RENDER);
    }

    private final DoubleSetting scale = doubleSetting("缩放", 3.5, 1.0, 5.0, 0.1);
    private final DoubleSetting glowRadius = doubleSetting("发光半径", 10.0f, 1.0f, 10.0f, 0.5f);
    private final DoubleSetting glowIntensity = doubleSetting("发光强度", 1.0, 1.0, 5.0, 1.0);

    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);

    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {
        if (nullCheck()) return;

        TextRenderer textRenderer = textRendererSupplier.get();

        textRenderer.addGlowingText("Lumin", 4.0f, 4.0f, scale.getValue().floatValue(), new Color(0x7BE88EE3, true), glowRadius.getValue().floatValue(), glowIntensity.getValue().intValue(), StaticFontLoader.REGULAR);

        textRenderer.drawAndClear();
    }
}