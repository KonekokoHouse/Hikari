package com.github.lumin.modules.impl.player;

import com.github.lumin.managers.AltRotationManager;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.EnumSetting;
import com.github.lumin.settings.impl.BoolSetting;

public class MoveFix extends Module {

    public static final MoveFix INSTANCE = new MoveFix();

    private enum Engine {
        Lumin,
        Alt
    }

    public final EnumSetting<Engine> engine = enumSetting("Engine", Engine.Alt);
    public final BoolSetting onlyWhileRotating = boolSetting("OnlyWhileRotating", true);

    private MoveFix() {
        super("MoveFix", Category.PLAYER);
    }

    public float getYaw() {
        if (engine.getValue() == Engine.Alt) {
            return AltRotationManager.INSTANCE.getYaw();
        }
        return RotationManager.INSTANCE.getYaw();
    }

    public boolean isFixActive() {
        if (!isEnabled()) return false;
        if (!onlyWhileRotating.getValue()) return true;
        if (engine.getValue() == Engine.Alt) {
            return AltRotationManager.INSTANCE.isActive();
        }
        return RotationManager.INSTANCE.isActive();
    }
}
