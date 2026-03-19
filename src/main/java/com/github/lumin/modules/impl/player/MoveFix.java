package com.github.lumin.modules.impl.player;

import com.github.lumin.managers.TwoBTwoTRotationManager;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.IntSetting;

public class MoveFix extends Module {

    public static final MoveFix INSTANCE = new MoveFix();

    private enum Engine {
        Lumin,
        TwoBTwoT
    }

    public final EnumSetting<Engine> engine = enumSetting("Engine", Engine.TwoBTwoT);
    public final BoolSetting onlyWhileRotating = boolSetting("OnlyWhileRotating", true);
    public final BoolSetting mouseSensFix = boolSetting("MouseSensFix", false);
    public final IntSetting preserveTicks = intSetting("PreserveTicks", 1, 0, 20, 1);

    private MoveFix() {
        super("MoveFix", Category.PLAYER);
    }

    public float getYaw() {
        if (engine.getValue() == Engine.TwoBTwoT) {
            return TwoBTwoTRotationManager.INSTANCE.getYaw();
        }
        return RotationManager.INSTANCE.getYaw();
    }

    public boolean isFixActive() {
        if (!isEnabled()) return false;
        if (!onlyWhileRotating.getValue()) return true;
        if (engine.getValue() == Engine.TwoBTwoT) {
            return TwoBTwoTRotationManager.INSTANCE.isActive();
        }
        return RotationManager.INSTANCE.isActive();
    }
}
