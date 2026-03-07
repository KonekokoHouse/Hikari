package com.github.lumin.gui.menu;

import com.github.lumin.graphics.LuminTexture;
import com.github.lumin.graphics.renderers.RectRenderer;
import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.renderers.TextureRenderer;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import com.github.lumin.utils.resources.ResourceLocationUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.io.InputStream;
import java.util.Optional;

public class MainMenuScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = ResourceLocationUtils.getIdentifier("textures/gui/mainmenu/1.png");

    private final RectRenderer rectRenderer = new RectRenderer();
    private final RoundRectRenderer roundRectRenderer = new RoundRectRenderer();
    private final TextRenderer textRenderer = new TextRenderer();
    private final TextureRenderer textureRenderer = new TextureRenderer();
    private final Minecraft mc = Minecraft.getInstance();
    private LuminTexture backgroundTexture;
    private boolean textureLoaded = false;
    private final Animation slideAnimation = new Animation(Easing.SMOOTH_STEP, 800);
    private boolean isFirstRender = true;

    public MainMenuScreen() {
        super(Component.literal("主菜单"));
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!textureLoaded) {
            try {
                Optional<Resource> resource = mc.getResourceManager().getResource(BACKGROUND_TEXTURE);
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().open(); NativeImage image = NativeImage.read(is)) {
                        GpuTexture texture = RenderSystem.getDevice().createTexture(
                                () -> "Lumin-Background: " + BACKGROUND_TEXTURE,
                                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                                TextureFormat.RGBA8,
                                image.getWidth(),
                                image.getHeight(),
                                1,
                                (int) Math.floor(Math.log(Math.max(image.getWidth(), image.getHeight()))) + 1
                        );
                        var view = RenderSystem.getDevice().createTextureView(texture);
                        var sampler = RenderSystem.getDevice().createSampler(
                                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                                FilterMode.LINEAR, FilterMode.LINEAR,
                                (int) Math.floor(Math.log(Math.max(image.getWidth(), image.getHeight()))) + 1,
                                java.util.OptionalDouble.empty()
                        );
                        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image);
                        backgroundTexture = new LuminTexture(texture, view, sampler);
                    }
                }
            } catch (Exception ignored) {
            }
            textureLoaded = true;
        }

        if (backgroundTexture != null) {
            textureRenderer.addQuadTexture(BACKGROUND_TEXTURE, 0, 0, width, height, 0, 0, 1, 1, Color.WHITE, true);
        }
        rectRenderer.addRect(0, 0, width, height, new Color(0, 0, 0, 100));

        if (isFirstRender) {
            slideAnimation.setValue(mouseX <= width * 0.15f ? 1.0f : 0.0f);
            isFirstRender = false;
        }
        slideAnimation.run(mouseX <= width * 0.15f ? 1.0f : 0.0f);

        textRenderer.addText("Lumin", 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + (200 / (float) mc.getWindow().getGuiScale() - textRenderer.getWidth("Lumin", 4.0f / (float) mc.getWindow().getGuiScale())) / 2f + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 180 / (float) mc.getWindow().getGuiScale(), 4.0f / (float) mc.getWindow().getGuiScale(), new Color(255, 255, 255, 255));
        textRenderer.addText("萝莉控NB", 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + (200 / (float) mc.getWindow().getGuiScale() - textRenderer.getWidth("萝莉控 On Top", 1.3f / (float) mc.getWindow().getGuiScale())) / 2f + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 180 / (float) mc.getWindow().getGuiScale() + textRenderer.getHeight(4.0f / (float) mc.getWindow().getGuiScale()) + 10 / (float) mc.getWindow().getGuiScale(), 1.3f / (float) mc.getWindow().getGuiScale(), new Color(180, 180, 180, 255));

        rectRenderer.addRect(-350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), 0, 30 / (float) mc.getWindow().getGuiScale() + (200 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() * 2) + 30 / (float) mc.getWindow().getGuiScale(), height, new Color(15, 15, 15, 200));
        roundRectRenderer.addRoundRect(30 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 40 / (float) mc.getWindow().getGuiScale() - 15 / (float) mc.getWindow().getGuiScale(), 200 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() * 2, (4 * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale())) + 15 / (float) mc.getWindow().getGuiScale() * 2 - 10 / (float) mc.getWindow().getGuiScale(), 12f / (float) mc.getWindow().getGuiScale(), new Color(25, 25, 25, 230));

        for (int i = 0; i < 4; i++) {
            if (mouseX >= 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()) && mouseX <= 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + 200 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()) && mouseY >= height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()) && mouseY <= height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()) + 40 / (float) mc.getWindow().getGuiScale()) {
                roundRectRenderer.addRoundRect(30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()), 200 / (float) mc.getWindow().getGuiScale(), 40 / (float) mc.getWindow().getGuiScale(), 8f / (float) mc.getWindow().getGuiScale(), new Color(60, 60, 60, 255));
                roundRectRenderer.addRoundRect(30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()), 200 / (float) mc.getWindow().getGuiScale(), 40 / (float) mc.getWindow().getGuiScale(), 8f / (float) mc.getWindow().getGuiScale(), new Color(255, 255, 255, 30));
            } else {
                roundRectRenderer.addRoundRect(30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()), 200 / (float) mc.getWindow().getGuiScale(), 40 / (float) mc.getWindow().getGuiScale(), 8f / (float) mc.getWindow().getGuiScale(), new Color(40, 40, 40, 255));
            }
            textRenderer.addText(new String[]{"单人游戏", "多人游戏", "设置", "退出游戏"}[i], 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + (200 / (float) mc.getWindow().getGuiScale() - textRenderer.getWidth(new String[]{"单人游戏", "多人游戏", "设置", "退出游戏"}[i], 1.5f / (float) mc.getWindow().getGuiScale())) / 2f + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()), height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()) + (40 / (float) mc.getWindow().getGuiScale() - textRenderer.getHeight(1.5f / (float) mc.getWindow().getGuiScale())) / 2f, 1.5f / (float) mc.getWindow().getGuiScale(), Color.WHITE);
        }

        textureRenderer.draw();
        rectRenderer.draw();
        roundRectRenderer.draw();
        textRenderer.draw();
        textureRenderer.clear();
        rectRenderer.clear();
        roundRectRenderer.clear();
        textRenderer.clear();
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean focused) {
        if (event.button() == 0) {
            for (int i = 0; i < 4; i++) {
                if (event.x() >= 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()) && event.x() <= 30 / (float) mc.getWindow().getGuiScale() + 15 / (float) mc.getWindow().getGuiScale() + 200 / (float) mc.getWindow().getGuiScale() + -350 / (float) mc.getWindow().getGuiScale() * (1 - slideAnimation.getValue()) && event.y() >= height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()) && event.y() <= height / 2f - 40 / (float) mc.getWindow().getGuiScale() + i * (40 / (float) mc.getWindow().getGuiScale() + 10 / (float) mc.getWindow().getGuiScale()) + 40 / (float) mc.getWindow().getGuiScale()) {
                    mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
                    switch (i) {
                        case 0 -> mc.setScreen(new SelectWorldScreen(this));
                        case 1 -> mc.setScreen(new JoinMultiplayerScreen(this));
                        case 2 -> mc.setScreen(new OptionsScreen(this, mc.options));
                        case 3 -> mc.stop();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }
}