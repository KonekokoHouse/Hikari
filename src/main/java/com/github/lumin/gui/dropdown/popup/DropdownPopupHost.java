package com.github.lumin.gui.dropdown.popup;

import com.github.lumin.gui.dropdown.DropdownLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class DropdownPopupHost {

    private Popup activePopup;

    public void open(Popup popup) {
        this.activePopup = popup;
    }

    public void close() {
        this.activePopup = null;
    }

    public Popup getActivePopup() {
        return activePopup;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (activePopup != null) {
            activePopup.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (activePopup == null) {
            return false;
        }
        if (!activePopup.getBounds().contains(event.x(), event.y())) {
            close();
            return true;
        }
        boolean handled = activePopup.mouseClicked(event, isDoubleClick);
        if (handled && activePopup.shouldCloseAfterClick()) {
            close();
        }
        return handled;
    }

    public boolean keyPressed(KeyEvent event) {
        if (activePopup == null) {
            return false;
        }
        if (event.key() == 256) {
            close();
            return true;
        }
        return activePopup.keyPressed(event);
    }

    public boolean charTyped(CharacterEvent event) {
        if (activePopup == null) {
            return false;
        }
        return activePopup.charTyped(event);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (activePopup == null) {
            return false;
        }
        activePopup.mouseReleased(event);
        return true;
    }

    public boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
        if (activePopup == null) {
            return false;
        }
        activePopup.mouseDragged(event, mouseX, mouseY);
        return true;
    }

    public interface Popup {
        DropdownLayout.Rect getBounds();

        void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

        boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick);

        default boolean shouldCloseAfterClick() {
            return false;
        }

        default boolean keyPressed(KeyEvent event) {
            return false;
        }

        default boolean charTyped(CharacterEvent event) {
            return false;
        }

        default boolean mouseReleased(MouseButtonEvent event) {
            return false;
        }

        default boolean mouseDragged(MouseButtonEvent event, double mouseX, double mouseY) {
            return false;
        }
    }
}
