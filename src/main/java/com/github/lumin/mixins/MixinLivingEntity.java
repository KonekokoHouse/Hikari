package com.github.lumin.mixins;

import com.github.lumin.events.FallFlyingEvent;
import com.github.lumin.events.JumpEvent;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.impl.player.JumpCooldown;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector2f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Shadow
    private int noJumpDelay;

    @Redirect(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float redirectGetYRotInJumpFromGround(LivingEntity instance) {
        if (instance == Minecraft.getInstance().player) {
            JumpEvent event = NeoForge.EVENT_BUS.post(new JumpEvent(instance.getYRot()));
            return event.getYaw();
        }
        return instance.getYRot();
    }

    @Redirect(method = "tickHeadTurn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float modifyHeadYaw(LivingEntity entity) {
        if (entity == Minecraft.getInstance().player) {
            Vector2f animationRotation = RotationManager.INSTANCE.animationRotation;
            if (animationRotation != null) {
                return animationRotation.x;
            }
        }
        return entity.getYRot();
    }
    @Redirect(
            method = {"updateFallFlyingMovement"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"
            )
    )
    private float d(LivingEntity instance) {
        FallFlyingEvent event = new FallFlyingEvent(instance.getXRot());
        NeoForge.EVENT_BUS.post(event);
        return event.getPitch();
    }
    @Redirect(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;noJumpDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void redirectJumpingCooldown(LivingEntity instance, int value) {
        JumpCooldown module = JumpCooldown.INSTANCE;
        if (instance == Minecraft.getInstance().player && module.isEnabled()) {
            this.noJumpDelay = module.cooldown.getValue();
        } else {
            this.noJumpDelay = value;
        }
    }

}
