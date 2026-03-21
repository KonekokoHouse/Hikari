package com.github.lumin.gui.dropdown.adapter;

import com.github.lumin.gui.dropdown.component.SettingRow;
import com.github.lumin.gui.dropdown.component.setting.*;
import com.github.lumin.settings.Setting;
import com.github.lumin.settings.impl.*;

public final class SettingViewFactory {

    private SettingViewFactory() {
    }

    public static SettingRow<?> create(Setting<?> setting) {
        if (setting instanceof BoolSetting boolSetting) {
            return new BoolSettingRow(boolSetting);
        }
        if (setting instanceof EnumSetting<?> enumSetting) {
            return new EnumSettingRow(enumSetting);
        }
        if (setting instanceof IntSetting intSetting) {
            return new IntSettingRow(intSetting);
        }
        if (setting instanceof DoubleSetting doubleSetting) {
            return new DoubleSettingRow(doubleSetting);
        }
        if (setting instanceof ColorSetting colorSetting) {
            return new ColorSettingRow(colorSetting);
        }
        if (setting instanceof StringSetting stringSetting) {
            return new StringSettingRow(stringSetting);
        }
        return null;
    }

}
