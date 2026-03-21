package com.github.lumin.gui.dropdown.panel;

import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.ShadowRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.gui.dropdown.DropdownLayout;
import com.github.lumin.gui.dropdown.DropdownState;
import com.github.lumin.gui.dropdown.DropdownTheme;
import com.github.lumin.gui.dropdown.adapter.ModuleViewModel;
import com.github.lumin.gui.dropdown.component.ModuleRow;
import com.github.lumin.gui.dropdown.util.DropdownScissor;
import com.github.lumin.modules.Module;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleListPanel {

    protected final DropdownState state;
    private final RoundRectRenderer roundRectRenderer;
    private final RectRenderer rectRenderer;
    private final ShadowRenderer shadowRenderer;
    private final TextRenderer textRenderer;
    private DropdownLayout.Rect bounds;
    private int guiHeight;
    private final List<ModuleRow> rows = new ArrayList<>();
    private final Map<Module, Animation> hoverAnimations = new HashMap<>();
    private final Map<Module, Animation> selectionAnimations = new HashMap<>();

    public ModuleListPanel(DropdownState state, RoundRectRenderer roundRectRenderer, RectRenderer rectRenderer, ShadowRenderer shadowRenderer, TextRenderer textRenderer) {
        this.state = state;
        this.roundRectRenderer = roundRectRenderer;
        this.rectRenderer = rectRenderer;
        this.shadowRenderer = shadowRenderer;
        this.textRenderer = textRenderer;
    }

    public void render(GuiGraphics guiGraphics, DropdownLayout.Rect bounds, int mouseX, int mouseY, float partialTick) {
        this.bounds = bounds;
        this.guiHeight = guiGraphics.guiHeight();
        rows.clear();

        textRenderer.addText(state.getSelectedCategory().getName(), bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 10.0f, 0.78f, DropdownTheme.TEXT_PRIMARY, StaticFontLoader.DUCKSANS);
        textRenderer.addText("Modules", bounds.x() + DropdownTheme.PANEL_TITLE_INSET, bounds.y() + 21.0f, 0.56f, DropdownTheme.TEXT_SECONDARY);

        DropdownLayout.Rect viewport = getViewport();
        List<Module> modules = state.getVisibleModules();
        float contentHeight = modules.size() * (ModuleRow.HEIGHT + DropdownTheme.ROW_GAP);
        state.setMaxModuleScroll(contentHeight - viewport.height());

        DropdownScissor.apply(viewport, rectRenderer, roundRectRenderer, shadowRenderer, textRenderer, guiHeight);
        float y = viewport.y() - state.getModuleScroll();
        for (Module module : modules) {
            ModuleRow row = new ModuleRow(ModuleViewModel.from(module), new DropdownLayout.Rect(viewport.x(), y, viewport.width(), ModuleRow.HEIGHT));
            rows.add(row);
            Animation hoverAnimation = hoverAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 120L));
            Animation selectionAnimation = selectionAnimations.computeIfAbsent(module, ignored -> new Animation(Easing.EASE_OUT_CUBIC, 160L));
            hoverAnimation.run(row.getBounds().contains(mouseX, mouseY) ? 1.0f : 0.0f);
            selectionAnimation.run(state.getSelectedModule() == module ? 1.0f : 0.0f);
            row.render(roundRectRenderer, rectRenderer, textRenderer, hoverAnimation.getValue(), selectionAnimation.getValue());
            y += ModuleRow.HEIGHT + DropdownTheme.ROW_GAP;
        }
        DropdownScissor.clear(rectRenderer, roundRectRenderer, shadowRenderer, textRenderer);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (bounds == null || event.button() != 0) {
            return false;
        }
        for (ModuleRow row : rows) {
            if (!row.getBounds().contains(event.x(), event.y())) {
                continue;
            }
            if (row.getToggleBounds().contains(event.x(), event.y())) {
                row.getModule().module().toggle();
            } else {
                state.setSelectedModule(row.getModule().module());
            }
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        DropdownLayout.Rect viewport = getViewport();
        if (bounds != null && viewport.contains(mouseX, mouseY)) {
            state.scrollModules(-scrollY * 20.0f);
            return true;
        }
        return false;
    }

    private DropdownLayout.Rect getViewport() {
        return new DropdownLayout.Rect(bounds.x() + DropdownTheme.PANEL_VIEWPORT_INSET, bounds.y() + 34.0f, bounds.width() - DropdownTheme.PANEL_VIEWPORT_INSET * 2.0f, bounds.height() - 40.0f);
    }
}
