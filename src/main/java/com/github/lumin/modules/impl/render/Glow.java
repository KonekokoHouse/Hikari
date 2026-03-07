package com.github.lumin.modules.impl.render;

import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.awt.*;

public class Glow extends Module {

    public static final Glow INSTANCE = new Glow();

    private final BoolSetting targetSelf = boolSetting("自己", true);
    private final BoolSetting targetPlayers = boolSetting("其他玩家", true);
    private final BoolSetting targetMonsters = boolSetting("怪物", true);
    private final BoolSetting targetMobs = boolSetting("生物", true);

    private Glow() {
        super("发光", "Glow", Category.RENDER);
    }

    public boolean shouldRenderGlow(LivingEntity entity) {
        if (entity == mc.player) {
            return targetSelf.getValue();
        }

        if (entity instanceof Player) {
            return targetPlayers.getValue();
        }

        if (entity instanceof Monster) {
            return targetMonsters.getValue();
        }

//        if (entity instanceof EnderDragon) {
//            return targetMonsters.getValue();
//        }

        return targetMobs.getValue();
    }

    public int getGlowColor(LivingEntity entity) {
        if (entity == mc.player) {
            return getRainbowColor();
        }

//        if (entity instanceof EnderDragon) {
//            return 0xFFAA00FF;
//        }

        if (entity instanceof Monster) {
            return Color.RED.getRGB();
        }

        return 0xFFFFFFFF;
    }

    private int getRainbowColor() {
        float hue = (System.currentTimeMillis() % 3000) / 3000.0f;
        return Color.HSBtoRGB(hue, 1.0f, 1.0f);
    }
}