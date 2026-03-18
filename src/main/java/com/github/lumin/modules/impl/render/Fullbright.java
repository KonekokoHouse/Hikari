package com.github.lumin.modules.impl.render;


import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.EnumSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class Fullbright extends Module {

    public static final Fullbright INSTANCE = new Fullbright();

    private Fullbright() {
        super("Fullbright", Category.RENDER);
    }

    private enum Mode {
        Gamma,
        Potion
    }

    private final EnumSetting<Mode> mode = enumSetting("Mode", Mode.Gamma);

    public boolean isGammaMode() {
        return isEnabled() && mode.is(Mode.Gamma);
    }

    @Override
    public void onDisable() {
        if (nullCheck() || !mode.is(Mode.Gamma)) return;
        mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        if (nullCheck() || !mode.is(Mode.Gamma)) return;
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
    }

}
