package com.github.lumin.modules.impl.client;

import com.github.lumin.gui.clickgui.ClickGuiScreen;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.ModeSetting;

public class ClickGui extends Module {

    public static final ClickGui INSTANCE = new ClickGui();

    public ClickGui() {
        super("控制面板", "idk", Category.CLIENT);
    }

    public final DoubleSetting scale = doubleSetting("界面缩放", 1.0, 0.5, 2.0, 0.05);

//    public final ColorSetting mainColor = colorSetting("主色调", new Color(255, 183, 197, 255));
//    public final ColorSetting secondColor = colorSetting("次色调", new Color(255, 133, 161, 255));
//    public final ColorSetting backgroundColor = colorSetting("背景颜色", new Color(28, 28, 28, 120));
//    public final ColorSetting expandedBackgroundColor = colorSetting("展开背景颜色", new Color(20, 20, 20, 120));

    public final BoolSetting backgroundBlur = boolSetting("背景模糊", true);
    public final DoubleSetting blurStrength = doubleSetting("模糊强度", 100.0, 1.0, 20.0, 0.5, backgroundBlur::getValue, true);
    public final ModeSetting blurMode = modeSetting("模糊方式", "仅侧边栏", new String[]{"全屏", "仅侧边栏"}, backgroundBlur::getValue);

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