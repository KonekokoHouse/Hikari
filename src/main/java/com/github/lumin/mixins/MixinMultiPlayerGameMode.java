package com.github.lumin.mixins;

import com.github.lumin.Lumin;
import com.github.lumin.managers.RotationManager;
import com.github.lumin.modules.impl.player.BreakCooldown;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Unique
    private float arknights$savedYaw;
    @Unique
    private float arknights$savedPitch;
    @Unique
    private boolean arknights$rotationModified;

    @Inject(method = "useItem", at = @At("HEAD"))
    private void preUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (RotationManager.INSTANCE.isActive() && RotationManager.INSTANCE.rotations != null) {
            arknights$savedYaw = player.getYRot();
            arknights$savedPitch = player.getXRot();
            player.setYRot(RotationManager.INSTANCE.getYaw());
            player.setXRot(RotationManager.INSTANCE.getPitch());
            arknights$rotationModified = true;
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void postUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (arknights$rotationModified) {
            player.setYRot(arknights$savedYaw);
            player.setXRot(arknights$savedPitch);
            arknights$rotationModified = false;
        }
    }
    @Shadow
    private int destroyDelay;

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 1))
    private void creativeBreakDelayChange(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD, ordinal = 2))
    private void survivalBreakDelayChange(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

    @Redirect(method = "startDestroyBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;destroyDelay:I", opcode = Opcodes.PUTFIELD))
    private void creativeBreakDelayChange2(MultiPlayerGameMode instance, int value) {
        BreakCooldown breakCooldown = BreakCooldown.INSTANCE;
        if (breakCooldown.isEnabled()) {
            destroyDelay = breakCooldown.cooldown.getValue();
        } else {
            destroyDelay = value;
        }
    }

}
