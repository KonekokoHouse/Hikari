package com.github.lumin.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public class MixinMinecraft {
//    @Inject(
//            method = {"shouldEntityAppearGlowing"},
//            at = {@At("RETURN")},
//            cancellable = true
//    )
//    private void shouldEntityAppearGlowing(Entity pEntity, CallbackInfoReturnable<Boolean> cir) {
//        if (Glow.INSTANCE.isEnabled() && pEntity instanceof Player) {
//            cir.setReturnValue(true);
//        }
//    }

}
