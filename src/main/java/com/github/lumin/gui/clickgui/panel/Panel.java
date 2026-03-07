package com.github.lumin.gui.clickgui.panel;

import com.github.lumin.graphics.renderers.*;
import com.github.lumin.gui.IComponent;
import com.github.lumin.modules.impl.client.ClickGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.*;

public class Panel implements IComponent {

    private final Minecraft mc = Minecraft.getInstance();

    private final RoundRectRenderer bottomRoundRect = new RoundRectRenderer();
    private final RoundRectRenderer topRoundRect = new RoundRectRenderer();
    private final TextureRenderer textureRenderer = new TextureRenderer();
    private final TextRenderer fontRenderer = new TextRenderer();
    private final ShadowRenderer shadowRenderer = new ShadowRenderer();

    private final RendererSet set = new RendererSet(bottomRoundRect, topRoundRect, textureRenderer, fontRenderer, null, null, null, null);

    private final Sidebar sidebar = new Sidebar();
    private final ContentPanel contentPanel = new ContentPanel();


    public Panel() {
        sidebar.setOnSelect(contentPanel::setCurrentCategory);
        contentPanel.setCurrentCategory(sidebar.getSelectedCategory());
    }

    public void render(RendererSet set, int mouseX, int mouseY, float deltaTicks, float alpha) {

        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();

        if (ClickGui.INSTANCE.blurMode.is("全屏")) {
            if (ClickGui.INSTANCE.backgroundBlur.getValue()) {
                BlurRenderer.INSTANCE.drawBlur(0.0f, 0.0f, screenWidth, screenHeight, 0.0f, ClickGui.INSTANCE.blurStrength.getValue().floatValue());
            } else {
                bottomRoundRect.addRoundRect(0.0f, 0.0f, screenWidth, screenHeight, 0.0f, new Color(18, 18, 18, (int) (110 * alpha)));
            }
        }

        float targetWidth = screenWidth * 0.5f;
        float minWidth = 400f * guiScale;
        float width = Math.max(targetWidth, minWidth);
        float height = width * 9.0f / 16.0f;

        if (height > screenHeight * 0.9f) {
            height = screenHeight * 0.9f;
            width = height * 16.0f / 9.0f;
        }

        float scaledWidth = width;
        float scaledHeight = height;
        float x = (screenWidth - scaledWidth) / 2.0f;
        float y = (screenHeight - scaledHeight) / 2.0f;

        shadowRenderer.addShadow(x, y, scaledWidth, scaledHeight, 20f * guiScale, 12f * guiScale, new Color(0, 0, 0, 100));
        shadowRenderer.drawAndClear();

        float sidebarWidth = Math.max(120f * guiScale, width / 4);
        float contentWidth = width - sidebarWidth;

        sidebar.setBounds(x, y, sidebarWidth, height);
        contentPanel.setBounds(x + sidebarWidth, y, contentWidth, height);
        sidebar.render(this.set, mouseX, mouseY, deltaTicks, alpha);
        contentPanel.render(this.set, mouseX, mouseY, deltaTicks, alpha);

        bottomRoundRect.drawAndClear();
        topRoundRect.drawAndClear();
        textureRenderer.drawAndClear();
        fontRenderer.drawAndClear();

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        return sidebar.mouseClicked(event, focused) || contentPanel.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return sidebar.mouseReleased(event) || contentPanel.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return sidebar.keyPressed(event) || contentPanel.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return sidebar.charTyped(event) || contentPanel.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return sidebar.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || contentPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

}