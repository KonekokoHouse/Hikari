package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;

public class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    public final BoolSetting potionEffects = boolSetting("药水效果", true);
    public final BoolSetting playerNameTags = boolSetting("玩家名牌", true);

    private NoRender() {
        super("NoRender", Category.RENDER);
    }
}