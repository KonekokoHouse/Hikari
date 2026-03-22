package com.github.lumin.modules.impl.client;

import com.github.lumin.gui.dropdown.DropdownScreen;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.EnumSetting;

public class ClickGui extends Module {

    public enum ThemePreset {
        TonalSpot,
        Neutral,
        Vibrant,
        Expressive,
        Fidelity,
        Content,
        Rainbow,
        FruitSalad,
        Monochrome
    }

    public enum ThemeMode {
        Dark,
        Light
    }

    public static final ClickGui INSTANCE = new ClickGui();

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
    }

    private final BoolSetting backgroundBlur = boolSetting("BackgroundBlur", true);
    public final EnumSetting<ThemeMode> themeMode = enumSetting("ThemeMode", ThemeMode.Dark);
    public final EnumSetting<ThemePreset> themePreset = enumSetting("ThemePreset", ThemePreset.TonalSpot);

    @Override
    protected void onEnable() {
        if (nullCheck()) return;
        mc.setScreen(new DropdownScreen());
    }

    @Override
    protected void onDisable() {
        if (mc.screen instanceof DropdownScreen) {
            mc.setScreen(null);
        }
    }

    public boolean shouldBlur() {
        return backgroundBlur.getValue();
    }

    public ThemePreset getThemePreset() {
        return themePreset.getValue();
    }

    public ThemeMode getThemeMode() {
        return themeMode.getValue();
    }

}
