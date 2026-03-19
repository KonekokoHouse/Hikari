package com.github.lumin.managers;

import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.modules.impl.combat.AimAssist;
import com.github.lumin.modules.impl.combat.AntiBot;
import com.github.lumin.modules.impl.combat.AutoClicker;
import com.github.lumin.modules.impl.combat.AutoCrystal;
import com.github.lumin.modules.impl.combat.FakePlayer;
import com.github.lumin.modules.impl.combat.KillAura;
import com.github.lumin.modules.impl.player.*;
import com.github.lumin.modules.impl.render.*;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

public class ModuleManager {

    public static final ModuleManager INSTANCE = new ModuleManager();

    private List<Module> modules;

    private ModuleManager() {
    }

    public void initModules() {
        modules = List.of(

                // Client
                ClickGui.INSTANCE,

                // Combat
                AimAssist.INSTANCE,
                AntiBot.INSTANCE,
                AutoClicker.INSTANCE,
                AutoCrystal.INSTANCE,
                FakePlayer.INSTANCE,
                KillAura.INSTANCE,

                // Player
                AutoAccount.INSTANCE,
                BreakCooldown.INSTANCE,
                Disabler.INSTANCE,
                InvManager.INSTANCE,
                JumpCooldown.INSTANCE,
                MoveFix.INSTANCE,
                NoSlow.INSTANCE,
                SafeWalk.INSTANCE,
                Scaffold.INSTANCE,
                Speedmine.INSTANCE,
                Sprint.INSTANCE,
                Stealer.INSTANCE,
                Stuck.INSTANCE,
                Velocity.INSTANCE,

                // Render
                ESP.INSTANCE,
                Fullbright.INSTANCE,
                HUD.INSTANCE,
                ModuleList.INSTANCE,
                Nametags.INSTANCE,
                NoRender.INSTANCE

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
