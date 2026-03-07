package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.render.WorldToScreen;
import com.google.common.base.Suppliers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Nametags extends Module {

    public static final Nametags INSTANCE = new Nametags();

    public Nametags() {
        super("名牌显示", "Nametags", Category.RENDER);
    }

    private final IntSetting range = intSetting("距离", 64, 8, 256, 1);
    private final BoolSetting showSelf = boolSetting("显示自己", false);
    private final BoolSetting showItems = boolSetting("显示装备", false);
    private final BoolSetting showHealthText = boolSetting("显示血量数值", true);
    private final ColorSetting backgroundColor = colorSetting("背景颜色", new Color(0, 0, 0, 140));
    private final ColorSetting textColor = colorSetting("文字颜色", Color.WHITE);

    private final Supplier<RoundRectRenderer> roundRectRendererSupplier = Suppliers.memoize(RoundRectRenderer::new);
    private final Supplier<TextRenderer> textRendererSupplier = Suppliers.memoize(TextRenderer::new);

    private final List<TagInfo> tags = new ArrayList<>();

    @SubscribeEvent
    private void onRenderGui(RenderGuiEvent.Post event) {
        if (nullCheck()) return;
        if (tags.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        RoundRectRenderer roundRectRenderer = roundRectRendererSupplier.get();
        TextRenderer textRenderer = textRendererSupplier.get();

        Color bg = backgroundColor.getValue();
        Color fg = textColor.getValue();

        for (TagInfo tag : tags) {
            float scale = tag.scale();
            float textW = textRenderer.getWidth(tag.text(), scale, StaticFontLoader.REGULAR);
            float textH = textRenderer.getHeight(scale, StaticFontLoader.REGULAR);
            float hpW = showHealthText.getValue() ? textRenderer.getWidth(tag.healthText(), scale, StaticFontLoader.REGULAR) : 0.0f;
            float topLineW = (showHealthText.getValue() && hpW > 0.0f) ? textW + 4.0f + hpW : textW;
            float boxW = topLineW + 8.0f;
            float boxH = textH + 8.0f;
            float boxLeft = tag.x() - boxW * 0.5f;
            float boxTop = tag.y() - boxH - 8.0f;
            float cursorY = boxTop + 4.0f;

            if (showItems.getValue() && !tag.items().isEmpty()) {
                float itemRowW = (tag.items().size() * 16.0f + (tag.items().size() - 1) * 2.0f) * scale;
                float itemsLeft = tag.x() - itemRowW * 0.5f;
                float itemY = boxTop - 16.0f * scale - 2.0f;

                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(itemsLeft, itemY);
                guiGraphics.pose().scale(scale, scale);
                guiGraphics.pose().translate(-itemsLeft, -itemY);

                int seed = 0;
                for (int i = 0; i < tag.items().size(); i++) {
                    ItemStack stack = tag.items().get(i);
                    if (stack == null || stack.isEmpty()) continue;
                    guiGraphics.renderItem(mc.player, stack, (int) (itemsLeft + i * 18.0f), (int) itemY, seed++);
                    guiGraphics.renderItemDecorations(mc.font, stack, (int) (itemsLeft + i * 18.0f), (int) itemY);
                }

                guiGraphics.pose().popMatrix();
            }

            roundRectRenderer.addRoundRect(boxLeft, boxTop, boxW, boxH, 6.0f * scale, bg);
            textRenderer.addText(tag.text(), tag.x() - topLineW * 0.5f, cursorY - 2, scale, fg, StaticFontLoader.REGULAR);

            if (showHealthText.getValue() && hpW > 0.0f) {
                textRenderer.addText(tag.healthText(), tag.x() - topLineW * 0.5f + textW + 4.0f, cursorY - 2, scale, tag.healthColor(), StaticFontLoader.REGULAR);
            }
        }

        roundRectRenderer.drawAndClear();
        textRenderer.drawAndClear();
    }


    @SubscribeEvent
    private void onRenderAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        if (nullCheck()) return;

        tags.clear();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = mc.getEntityRenderDispatcher().camera.position();

        var window = mc.getWindow();
        float guiWidth = (float) window.getWidth() / (float) window.getGuiScale();
        float guiHeight = (float) window.getHeight() / (float) window.getGuiScale();

        float aspect = (float) window.getWidth() / (float) window.getHeight();

        int fovDeg = mc.options.fov().get();
        float fovRad = (float) Math.toRadians(fovDeg);

        float far = (float) Math.max(256.0, mc.gameRenderer.getRenderDistance());
        Matrix4f projection = new Matrix4f().setPerspective(fovRad, aspect, 0.05f, far);
        Matrix4f modelViewRotation = new Matrix4f(event.getModelViewMatrix());

        float maxRange = range.getValue();

        for (Player player : mc.level.players()) {
            if (!showSelf.getValue() && player == mc.player) continue;

            Vec3 playerPos = player.getPosition(partialTick);
            float dist = (float) playerPos.distanceTo(cameraPos);
            if (dist > maxRange) continue;

            Vec3 headPos = playerPos.add(0.0, player.getBbHeight() + 0.35, 0.0);
            Vector2f screen = WorldToScreen.projectToGui(headPos, cameraPos, modelViewRotation, projection, guiWidth, guiHeight);
            if (screen == null) continue;

            if (screen.x < -64.0f || screen.y < -64.0f || screen.x > guiWidth + 64.0f || screen.y > guiHeight + 64.0f)
                continue;

            String text = player.getName().getString();
            float scale = Math.max(0.65f, 1.0f - (dist / maxRange) * 0.35f);

            float maxHealth = player.getMaxHealth();
            float health = player.getHealth() + player.getAbsorptionAmount();
            float frac = maxHealth > 0.0f ? health / maxHealth : 0.0f;
            String hpText = String.format("%.1f", health);
            Color hpColor = getHealthColor(frac);

            List<ItemStack> items = new ArrayList<>();
            if (showItems.getValue()) {
                ItemStack off = player.getOffhandItem();
                if (!off.isEmpty()) items.add(off);

                ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
                ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
                ItemStack feet = player.getItemBySlot(EquipmentSlot.FEET);

                if (!head.isEmpty()) items.add(head);
                if (!chest.isEmpty()) items.add(chest);
                if (!legs.isEmpty()) items.add(legs);
                if (!feet.isEmpty()) items.add(feet);

                ItemStack main = player.getMainHandItem();
                if (!main.isEmpty()) items.add(main);
            }

            tags.add(new TagInfo(text, hpText, hpColor, items, screen.x, screen.y + 5, scale));
        }
    }

    private record TagInfo(String text, String healthText, Color healthColor, List<ItemStack> items, float x, float y,
                           float scale) {
    }

    private static Color getHealthColor(float frac) {
        frac = Mth.clamp(frac, 0.0f, 1.0f);
        if (frac > 0.5f) {
            float t = (frac - 0.5f) * 2.0f;
            return lerpColor(new Color(255, 255, 0), new Color(0, 255, 0), t);
        } else {
            float t = frac * 2.0f;
            return lerpColor(new Color(255, 0, 0), new Color(255, 255, 0), t);
        }
    }

    private static Color lerpColor(Color a, Color b, float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(r, g, bl, al);
    }

}

