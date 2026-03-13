package com.github.lumin.modules.impl.player;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.IntSetting;

public class JumpCooldown extends Module {

    public static final JumpCooldown INSTANCE = new JumpCooldown();

    private JumpCooldown() {
        super("JumpCooldown", Category.PLAYER);
    }

    public final IntSetting cooldown = intSetting("冷却时间", 0, 0, 9, 1);

}
