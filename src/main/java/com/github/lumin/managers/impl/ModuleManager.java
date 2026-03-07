package com.github.lumin.managers.impl;

import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.modules.impl.combat.AimAssist;
import com.github.lumin.modules.impl.combat.AntiBot;
import com.github.lumin.modules.impl.combat.AutoClicker;
import com.github.lumin.modules.impl.combat.KillAura;
import com.github.lumin.modules.impl.player.*;
import com.github.lumin.modules.impl.render.FullBright;
import com.github.lumin.modules.impl.render.Glow;
import com.github.lumin.modules.impl.render.Nametags;
import com.github.lumin.modules.impl.render.RenderTest;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class ModuleManager {
    private List<Module> modules;

    public ModuleManager() {
        initModules();
    }

    private void initModules() {
        modules = List.of(

                // Client
                ClickGui.INSTANCE,

                // Combat
                AimAssist.INSTANCE,
                AntiBot.INSTANCE,
                AutoClicker.INSTANCE,
                KillAura.INSTANCE,

                // Player
                AutoKouZi.INSTANCE,
                BreakCooldown.INSTANCE,
                Disabler.INSTANCE,
                JumpCooldown.INSTANCE,
                NoSlow.INSTANCE,
                SafeWalk.INSTANCE,
                Scaffold.INSTANCE,
                Sprint.INSTANCE,
                Stuck.INSTANCE,

                // Visual
                FullBright.INSTANCE,
                Glow.INSTANCE,
                Nametags.INSTANCE,
                RenderTest.INSTANCE

        );
    }

    public List<Module> getModules() {
        return modules;
    }

    public void onKeyEvent(int keyCode, int action) {
        for (final var module : modules) {
            if (module.getKeyBind() == keyCode) {
                if (module.getBindMode() == Module.BindMode.Hold) {
                    if (action == InputConstants.PRESS || action == InputConstants.REPEAT) {
                        module.setEnabled(true);
                    } else if (action == InputConstants.RELEASE) {
                        module.setEnabled(false);
                    }
                } else {
                    if (action == InputConstants.PRESS) {
                        module.toggle();
                    }
                }
            }
        }
    }

}
