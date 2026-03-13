package com.github.lumin.settings.impl;

import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;

public class StringSetting extends Setting<String> {

    public StringSetting(String name, Module module, String defaultValue) {
        this(name, module, defaultValue, () -> true);
    }

    public StringSetting(String name, Module module, String defaultValue, Dependency dependency) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

}