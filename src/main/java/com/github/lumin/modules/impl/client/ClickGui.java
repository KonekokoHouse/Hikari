package com.github.lumin.modules.impl.client;

import com.github.lumin.gui.clickgui.ClickGuiScreen;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.EnumSetting;

import java.awt.*;

public class ClickGui extends Module {

    public static final ClickGui INSTANCE = new ClickGui();

    private ClickGui() {
        super("ClickGui", Category.CLIENT);
    }

    public final DoubleSetting scale = doubleSetting("Scale", 1.0, 0.5, 2.0, 0.05);
    public final ColorSetting shadowColor = colorSetting("ShadowColor", new Color(0, 0, 0, 113));

    public final BoolSetting backgroundBlackColor = boolSetting("BackgroundBlackColor", true);
    private final BoolSetting backgroundBlur = boolSetting("BackgroundBlur", true);
    private final DoubleSetting blurStrength = doubleSetting("BlurStrength", 10.5, 1.0, 15, 0.5, backgroundBlur::getValue);
    private final EnumSetting<BlurMode> blurMode = enumSetting("BlurMode", BlurMode.FullScreen, backgroundBlur::getValue);

    @Override
    protected void onEnable() {
        if (nullCheck()) return;
        mc.setScreen(new ClickGuiScreen());
    }

    @Override
    protected void onDisable() {
        if (mc.screen instanceof ClickGuiScreen) {
            mc.setScreen(null);
        }
    }

    public boolean shouldBlur() {
        return backgroundBlur.getValue();
    }

    public boolean isFullScreenBlur() {
        return shouldBlur() && blurMode.is(BlurMode.FullScreen);
    }

    public boolean isSidebarBlur() {
        return shouldBlur() && blurMode.is(BlurMode.OnlySideBar);
    }

    public float getBlurStrength() {
        return blurStrength.getValue().floatValue();
    }

    private enum BlurMode {
        FullScreen,
        OnlySideBar,
    }

}
