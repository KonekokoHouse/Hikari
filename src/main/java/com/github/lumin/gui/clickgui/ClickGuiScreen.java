package com.github.lumin.gui.clickgui;

import com.github.lumin.gui.clickgui.component.impl.ColorSettingComponent;
import com.github.lumin.gui.clickgui.panel.Panel;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ClickGuiScreen extends Screen {

    private final Panel panel = new Panel();

    private final Animation openAnimation = new Animation(Easing.EASE_OUT_QUAD, 300);

    public ClickGuiScreen() {
        super(Component.literal("ClickGui"));
    }

    @Override
    protected void init() {
        openAnimation.setStartValue(0f);
        openAnimation.run(1f);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        openAnimation.run(1f);
        panel.render(null, mouseX, mouseY, partialTick, openAnimation.getValue());
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean focused) {
        return panel.mouseClicked(event, focused) || super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(@NonNull MouseButtonEvent event) {
        return panel.mouseReleased(event) || super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        return panel.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NonNull CharacterEvent event) {
        return panel.charTyped(event) || super.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return panel.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        ColorSettingComponent.closeActivePicker();
        ClickGui.INSTANCE.setEnabled(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

}