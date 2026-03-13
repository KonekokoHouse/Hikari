package com.github.lumin.modules.impl.render;


import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.DoubleSetting;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class FullBright extends Module {

    public static final FullBright INSTANCE = new FullBright();

    private final DoubleSetting amount = doubleSetting("Amount", 1, 0.1f, 1, 0.1f);

    private FullBright() {
        super("FullBright", Category.RENDER);
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre e) {
        if (nullCheck()) return;
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
    }

    @Override
    public void onDisable() {
        if (mc.player != null && mc.level != null) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

}
