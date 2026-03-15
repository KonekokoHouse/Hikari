package com.github.lumin.mixins;

import com.github.lumin.modules.impl.render.Fullbright;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.ARGB;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public class MixinLightTexture {

    @Shadow
    @Final
    private GpuTexture texture;

    @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onUpdateLightTexture(float partialTicks, CallbackInfo ci, @Local ProfilerFiller profiler) {
        if (Fullbright.INSTANCE.isGammaMode()) {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(texture, ARGB.color(255, 255, 255, 255));
            profiler.pop();
            ci.cancel();
        }
    }

}
