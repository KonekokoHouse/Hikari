package com.github.lumin.managers;

import com.github.lumin.managers.impl.ConfigManager;
import com.github.lumin.managers.impl.ModuleManager;
import com.github.lumin.managers.impl.RotationManager;
import com.github.lumin.managers.impl.TranslateManager;

public class Managers {

    public static ConfigManager CONFIG;
    public static ModuleManager MODULE;
    public static RotationManager ROTATION;
    public static TranslateManager TRANSLATE;

    public static void initManagers() {

        MODULE = ModuleManager.INSTANCE;
        ROTATION = RotationManager.INSTANCE;
        CONFIG = ConfigManager.INSTANCE;
        TRANSLATE = TranslateManager.INSTANCE;

    }

}
