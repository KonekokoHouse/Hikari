package com.github.lumin.gui.dropdown;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.shaders.BlurShader;
import com.github.lumin.gui.dropdown.input.DropdownInputRouter;
import com.github.lumin.gui.dropdown.panel.CategoryRailPanel;
import com.github.lumin.gui.dropdown.panel.ModuleDetailPanel;
import com.github.lumin.gui.dropdown.panel.ModuleListPanel;
import com.github.lumin.gui.dropdown.popup.DropdownPopupHost;
import com.github.lumin.modules.impl.client.ClickGui;
import com.google.common.base.Suppliers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.Supplier;

public class DropdownScreen extends Screen {

    private final DropdownState state = new DropdownState();
    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);
    private final Supplier<RectRenderer> rectRendererSupplier = Suppliers.memoize(RectRenderer::new);
    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);
    private final Supplier<ShadowRenderer> shadowRendererSupplier = Suppliers.memoize(ShadowRenderer::new);
    private final DropdownPopupHost popupHost = new DropdownPopupHost();
    private final DropdownInputRouter inputRouter = new DropdownInputRouter();
    private final CategoryRailPanel categoryRailPanel = new CategoryRailPanel(state, rectRendererSupplier.get(), roundRectRendererSupplier.get(), textRendererSupplier.get());
    private final ModuleListPanel moduleListPanel = new ModuleListPanel(state, roundRectRendererSupplier.get(), rectRendererSupplier.get(), shadowRendererSupplier.get(), textRendererSupplier.get());
    private final ModuleDetailPanel moduleDetailPanel = new ModuleDetailPanel(state, roundRectRendererSupplier.get(), rectRendererSupplier.get(), shadowRendererSupplier.get(), textRendererSupplier.get());

    public DropdownScreen() {
        super(Component.literal("DropdownGui"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        float railWidth = categoryRailPanel.getAnimatedWidth();
        DropdownLayout.Layout layout = DropdownLayout.compute(width, height, railWidth);

        drawBackgroundScrim();
        drawChrome(layout);
        categoryRailPanel.render(guiGraphics, layout.rail(), mouseX, mouseY, partialTick);
        moduleListPanel.render(guiGraphics, layout.modules(), mouseX, mouseY, partialTick);
        moduleDetailPanel.render(guiGraphics, layout.detail(), mouseX, mouseY, partialTick);
        popupHost.render(guiGraphics, mouseX, mouseY, partialTick);

        shadowRendererSupplier.get().drawAndClear();
        roundRectRendererSupplier.get().drawAndClear();
        rectRendererSupplier.get().drawAndClear();
        textRendererSupplier.get().drawAndClear();
        categoryRailPanel.flushClippedText();
    }

    private void drawBackgroundScrim() {
        RectRenderer rectRenderer = rectRendererSupplier.get();
        rectRenderer.addRect(0, 0, width, height, DropdownTheme.SCRIM);
        rectRenderer.drawAndClear();
    }

    private void drawChrome(DropdownLayout.Layout layout) {
        ClickGui clickGui = ClickGui.INSTANCE;
        ShadowRenderer shadowRenderer = shadowRendererSupplier.get();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();

        if (clickGui.shouldBlur()) {
            BlurShader.INSTANCE.drawBlur(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), DropdownTheme.PANEL_RADIUS, clickGui.getBlurStrength());
        }

        shadowRenderer.addShadow(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), DropdownTheme.PANEL_RADIUS, 18.0f, DropdownTheme.SHADOW);
        roundRectRenderer.addRoundRect(layout.panel().x(), layout.panel().y(), layout.panel().width(), layout.panel().height(), DropdownTheme.PANEL_RADIUS, DropdownTheme.SURFACE);

        roundRectRenderer.addRoundRect(layout.rail().x(), layout.rail().y(), layout.rail().width(), layout.rail().height(), DropdownTheme.SECTION_RADIUS, DropdownTheme.SURFACE_DIM);
        roundRectRenderer.addRoundRect(layout.modules().x(), layout.modules().y(), layout.modules().width(), layout.modules().height(), DropdownTheme.SECTION_RADIUS, DropdownTheme.SURFACE_DIM);
        roundRectRenderer.addRoundRect(layout.detail().x(), layout.detail().y(), layout.detail().width(), layout.detail().height(), DropdownTheme.SECTION_RADIUS, DropdownTheme.SURFACE_DIM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, isDoubleClick);
        }

        DropdownLayout.Layout layout = DropdownLayout.compute(width, height, categoryRailPanel.getAnimatedWidth());
        if (!layout.panel().contains(mouseX, mouseY)) {
            onClose();
            return true;
        }
        return inputRouter.routeMouseClicked(event, isDoubleClick, popupHost, moduleDetailPanel, moduleListPanel, categoryRailPanel)
                || super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (moduleListPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (moduleDetailPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            onClose();
            return true;
        }
        if (inputRouter.routeKeyPressed(event, popupHost, moduleDetailPanel)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (Minecraft.getInstance().screen == this) {
            ClickGui.INSTANCE.setEnabled(false);
        }
        super.onClose();
    }

}
