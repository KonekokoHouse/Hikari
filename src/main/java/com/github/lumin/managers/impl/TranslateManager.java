package com.github.lumin.managers.impl;

import com.github.lumin.assets.i18n.TranslateComponent;

import java.util.ArrayList;
import java.util.List;

public class TranslateManager {

    public static TranslateManager INSTANCE = new TranslateManager();

    private final List<TranslateComponent> components = new ArrayList<>();

    private TranslateManager() {
    }

    public void refresh() {
        for (TranslateComponent component : components) {
            component.refresh();
        }
    }

    public void registerTranslateComponent(TranslateComponent component) {
        components.add(component);
    }

}
