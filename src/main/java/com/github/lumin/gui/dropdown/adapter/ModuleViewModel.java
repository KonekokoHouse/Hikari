package com.github.lumin.gui.dropdown.adapter;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;

public record ModuleViewModel(Module module, String displayName, String description, boolean enabled, Category category,
                              String searchText) {
    public static ModuleViewModel from(Module module) {
        String displayName = module.getTranslatedName();
        String description = module.getDescription();
        String searchText = (displayName + " " + description + " " + module.category.getName()).toLowerCase();
        return new ModuleViewModel(module, displayName, description, module.isEnabled(), module.category, searchText);
    }
}
