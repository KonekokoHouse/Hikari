package com.github.lumin.settings.impl;

import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;

import java.awt.*;

public class ColorSetting extends Setting<Color> {
    private final boolean allowAlpha;

    public ColorSetting(String name, Module module, Color defaultValue) {
        this(name, module, defaultValue, () -> true, true);
    }

    public ColorSetting(String name, Module module, Color defaultValue, boolean allowAlpha) {
        this(name, module, defaultValue, () -> true, allowAlpha);
    }

    public ColorSetting(String name, Module module, Color defaultValue, Dependency dependency) {
        this(name, module, defaultValue, dependency, true);
    }

    public ColorSetting(String name, Module module, Color defaultValue, Dependency dependency, boolean allowAlpha) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.allowAlpha = allowAlpha;
    }

    public boolean isAllowAlpha() {
        return allowAlpha;
    }
}