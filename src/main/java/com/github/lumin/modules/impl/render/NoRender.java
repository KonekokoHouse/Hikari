package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;

public class NoRender extends Module {

    public static final NoRender INSTANCE = new NoRender();

    public final BoolSetting potionEffects = boolSetting("PotionEffects", true);
    public final BoolSetting playerNameTags = boolSetting("PlayerNameTags", true);
    public final BoolSetting explosions = boolSetting("Explosions", true);
    public final BoolSetting totems = boolSetting("Totems", false);

    private NoRender() {
        super("NoRender", Category.RENDER);
    }
}
