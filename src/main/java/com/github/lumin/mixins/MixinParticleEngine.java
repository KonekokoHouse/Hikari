package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MixinParticleEngine {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void lumin$hideExplosionParticles(ParticleOptions particleOptions, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<Particle> cir) {
        if (!NoRender.INSTANCE.isEnabled() || !NoRender.INSTANCE.explosions.getValue()) {
            return;
        }
        if (particleOptions == null) {
            return;
        }
        if (particleOptions.getType() == ParticleTypes.EXPLOSION || particleOptions.getType() == ParticleTypes.EXPLOSION_EMITTER) {
            cir.setReturnValue(null);
        }
    }
}
