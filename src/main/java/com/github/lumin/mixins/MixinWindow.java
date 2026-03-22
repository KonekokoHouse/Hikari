package com.github.lumin.mixins;

import com.github.lumin.Lumin;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Mixin(Window.class)
public class MixinWindow {

    @Redirect(method = "setIcon", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/IconSet;getStandardIcons(Lnet/minecraft/server/packs/PackResources;)Ljava/util/List;"))
    private List<IoSupplier<InputStream>> onSetIcon(IconSet instance, PackResources resources) throws IOException {
        final InputStream stream16 = Lumin.class.getResourceAsStream("/assets/lumin/textures/icons/icon_16x16.png");
        final InputStream stream32 = Lumin.class.getResourceAsStream("/assets/lumin/textures/icons/icon_32x32.png");

        if (stream16 == null || stream32 == null) {
            return instance.getStandardIcons(resources);
        }

        return List.of(() -> stream16, () -> stream32);
    }

    @ModifyArg(method = "setTitle", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowTitle(JLjava/lang/CharSequence;)V", remap = false), index = 1)
    private CharSequence setTitle(CharSequence title) {
        return "Epsilon " + Lumin.VERSION;
    }

}
