package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.NoRender;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class MixinParticleManager {

    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void lumin$cancelExplosionParticles(ParticleOptions particleOptions, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (!shouldCancel(particleOptions)) {
            return;
        }
        cir.setReturnValue(null);
        cir.cancel();
    }

    @Inject(method = "addEmitter", at = @At("HEAD"), cancellable = true, require = 0)
    private void lumin$cancelExplosionEmitter(Entity entity, ParticleOptions particleOptions, int maxAge, CallbackInfo ci) {
        if (!shouldCancel(particleOptions)) {
            return;
        }
        ci.cancel();
    }

    private boolean shouldCancel(ParticleOptions particleOptions) {
        if (!NoRender.INSTANCE.isEnabled() || !NoRender.INSTANCE.explosions.getValue()) {
            return false;
        }
        if (particleOptions == null) {
            return false;
        }
        return particleOptions.getType() == ParticleTypes.EXPLOSION || particleOptions.getType() == ParticleTypes.EXPLOSION_EMITTER;
    }
}
