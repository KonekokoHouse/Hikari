package com.github.lumin.mixins;

import com.github.lumin.managers.RotationManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState> {

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;solveBodyRot(Lnet/minecraft/world/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f rotation = RotationManager.INSTANCE.animationRotation;
            Vector2f lastRotation = RotationManager.INSTANCE.lastAnimationRotation;
            if (RotationManager.INSTANCE.isActive() && rotation != null && lastRotation != null) {
                return Mth.rotLerp(tickDelta, lastRotation.x, rotation.x);
            }
        }
        return original;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float partialTick) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f rotation = RotationManager.INSTANCE.animationRotation;
            Vector2f lastRotation = RotationManager.INSTANCE.lastAnimationRotation;
            if (rotation != null && lastRotation != null && RotationManager.INSTANCE.isActive()) {
                float lastYaw = lastRotation.x;
                float currentYaw = rotation.x;
                float diff = Mth.wrapDegrees(currentYaw - lastYaw);
                return Mth.wrapDegrees(lastYaw + diff * partialTick);
            }
        }
        return original;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float partialTick) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f rotation = RotationManager.INSTANCE.animationRotation;
            Vector2f lastRotation = RotationManager.INSTANCE.lastAnimationRotation;
            if (rotation != null && lastRotation != null && RotationManager.INSTANCE.isActive()) {
                float lastPitch = lastRotation.y;
                float currentPitch = rotation.y;
                float diff = currentPitch - lastPitch;
                return lastPitch + diff * partialTick;
            }
        }
        return original;
    }

}
