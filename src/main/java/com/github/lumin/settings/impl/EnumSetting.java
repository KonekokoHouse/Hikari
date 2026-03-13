package com.github.lumin.settings.impl;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnumSetting<E extends Enum<E>> extends Setting<E> {

    private final Map<E, TranslateComponent> modeTranslations = new HashMap<>();
    private final E[] constants;

    public EnumSetting(String name, Module module, E defaultValue, Dependency dependency) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;

        Class<E> enumClass = defaultValue.getDeclaringClass();
        constants = enumClass.getEnumConstants();

        // lumin.modules.<module>.<setting>.<mode>
        String prefix = "modules." + module.getName().toLowerCase() + "." + name.toLowerCase();
        for (E m : constants) {
            modeTranslations.put(m, TranslateComponent.create(prefix, m.toString().toLowerCase()));
        }
    }

    public String getTranslatedValue() {
        TranslateComponent comp = modeTranslations.get(value);
        return comp != null ? comp.getTranslatedName() : value.toString();
    }

    public boolean is(String string) {
        return this.getValue().toString().equalsIgnoreCase(string);
    }

    public void setMode(String mode) {
        for (E e : constants) {
            if (Objects.equals(e.toString(), mode)) {
                setValue(e);
            }
        }
    }

    public void setMode(E mode) {
        setValue(mode);
    }

    public int getModeIndex() {
        int index = 0;
        for (E e : constants) {
            if (e == value) return index;
            index++;
        }
        return -1;
    }

    public E[] getModes() {
        return constants;
    }
}