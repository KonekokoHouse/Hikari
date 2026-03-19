package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevelSound {

    @Inject(method = "playLocalSound", at = @At("HEAD"), cancellable = true, require = 0)
    private void lumin$noExplosionSound(double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, boolean distanceDelay, CallbackInfo ci) {
        if (NoRender.INSTANCE.isEnabled() && NoRender.INSTANCE.explosions.getValue()) {
            sound.unwrapKey().ifPresent(key -> {
                String keyId = key.toString();
                if (keyId.contains("explode")) {
                    ci.cancel();
                }
            });
        }
    }
}
