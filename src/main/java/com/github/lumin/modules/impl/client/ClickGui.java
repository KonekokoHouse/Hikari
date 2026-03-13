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

    public final DoubleSetting scale = doubleSetting("界面缩放", 1.0, 0.5, 2.0, 0.05);
    public final ColorSetting shadowColor = colorSetting("阴影颜色", new Color(0, 0, 0, 113));

    public final BoolSetting backgroundBlackColor = boolSetting("黑色背景", true);
    public final BoolSetting backgroundBlur = boolSetting("背景模糊", true);
    public final DoubleSetting blurStrength = doubleSetting("模糊强度", 10.5, 1.0, 15, 0.5, backgroundBlur::getValue);
    public final EnumSetting<BlurMode> blurMode = enumSetting("模糊方式", BlurMode.FullScreen, backgroundBlur::getValue);

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

    public enum BlurMode {
        FullScreen,
        OnlySideBar,
    }

}

//    public static Color getMainColor() {
//        return INSTANCE.mainColor.getValue();
//    }
//
//    public static Color getSecondColor() {
//        return INSTANCE.secondColor.getValue();
//    }
//
//}