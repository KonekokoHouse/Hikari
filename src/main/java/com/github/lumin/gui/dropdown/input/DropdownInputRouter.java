package com.github.lumin.gui.dropdown.input;

import com.github.lumin.gui.dropdown.panel.CategoryRailPanel;
import com.github.lumin.gui.dropdown.panel.ModuleDetailPanel;
import com.github.lumin.gui.dropdown.panel.ModuleListPanel;
import com.github.lumin.gui.dropdown.popup.DropdownPopupHost;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class DropdownInputRouter {

    public boolean routeMouseClicked(MouseButtonEvent event, boolean isDoubleClick, DropdownPopupHost popupHost, ModuleDetailPanel detailPanel, ModuleListPanel moduleListPanel, CategoryRailPanel categoryRailPanel) {
        if (popupHost.mouseClicked(event, isDoubleClick)) {
            return true;
        }
        if (detailPanel.mouseClicked(event, isDoubleClick)) {
            return true;
        }
        if (moduleListPanel.mouseClicked(event, isDoubleClick)) {
            return true;
        }
        return categoryRailPanel.mouseClicked(event, isDoubleClick);
    }

    public boolean routeKeyPressed(KeyEvent event, DropdownPopupHost popupHost, ModuleDetailPanel detailPanel) {
        if (popupHost.keyPressed(event)) {
            return true;
        }
        return detailPanel.keyPressed(event);
    }

    public boolean routeMouseReleased(MouseButtonEvent event, DropdownPopupHost popupHost, ModuleDetailPanel detailPanel) {
        if (popupHost.getActivePopup() != null) {
            return false;
        }
        return detailPanel.mouseReleased(event);
    }

    public boolean routeMouseDragged(MouseButtonEvent event, double mouseX, double mouseY, DropdownPopupHost popupHost, ModuleDetailPanel detailPanel) {
        if (popupHost.getActivePopup() != null) {
            return false;
        }
        return detailPanel.mouseDragged(event, mouseX, mouseY);
    }

}
