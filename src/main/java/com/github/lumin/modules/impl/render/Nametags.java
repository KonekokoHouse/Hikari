package com.github.lumin.modules.impl.render;

import com.github.lumin.graphics.renderers.RoundRectRenderer;
import com.github.lumin.graphics.renderers.TextRenderer;
import com.github.lumin.graphics.text.StaticFontLoader;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.settings.impl.ColorSetting;
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
import org.joml.Vector4d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Nametags extends Module {

    public static final Nametags INSTANCE = new Nametags();

    private Nametags() {
        super("Nametags", Category.RENDER);
    }

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

        for (TagInfo tag : tags) {
            if (showItems.getValue() && !tag.items().isEmpty()) {
                float itemRowW = (tag.items().size() * 16.0f + (tag.items().size() - 1) * 2.0f) * tag.scale();
                float itemsLeft = tag.x() - itemRowW * 0.5f;
                float itemY = tag.y() - (textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f) - 8.0f - 7.0f * tag.scale() - 2.0f;

                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(itemsLeft, itemY);
                guiGraphics.pose().scale(tag.scale(), tag.scale());
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

            float hpW = showHealthText.getValue() ? textRenderer.getWidth(tag.healthText(), tag.scale(), StaticFontLoader.REGULAR) : 0.0f;
            float topLineW = (showHealthText.getValue() && hpW > 0.0f) ? textRenderer.getWidth(tag.text(), tag.scale(), StaticFontLoader.REGULAR) + 4.0f + hpW : textRenderer.getWidth(tag.text(), tag.scale(), StaticFontLoader.REGULAR);

            roundRectRenderer.addRoundRect(tag.x() - topLineW + 8.0f * 0.5f + 21.5F, tag.y() - textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8 - 15, topLineW + 8.0f, textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f, 6.0f * tag.scale(), backgroundColor.getValue());
            textRenderer.addGlowingText(tag.text(), tag.x() - topLineW * 0.5f, tag.y() - textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f - 8.0f + 4.0f - 9, tag.scale(), textColor.getValue(), 1.0f, (int) 2.0, StaticFontLoader.REGULAR);

            if (showHealthText.getValue() && hpW > 0.0f) {
                textRenderer.addGlowingText(tag.healthText(), tag.x() - topLineW * 0.5f + textRenderer.getWidth(tag.text(), tag.scale(), StaticFontLoader.REGULAR) + 4.0f, tag.y() - textRenderer.getHeight(tag.scale(), StaticFontLoader.REGULAR) + 8.0f - 8.0f + 4.0f - 9, tag.scale(), tag.healthColor(), 1.0f, (int) 2.0, StaticFontLoader.REGULAR);
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
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        float guiWidth = (float) mc.getWindow().getGuiScaledWidth();
        float guiHeight = (float) mc.getWindow().getGuiScaledHeight();

        for (Player player : mc.level.players()) {
            if (!showSelf.getValue() && player == mc.player) continue;

            Vec3 playerPos = player.getPosition(partialTick);
            float dist = (float) playerPos.distanceTo(cameraPos);
            if (dist > 256) continue;

            Vector4d screenPos = WorldToScreen.getEntityPositionsOn2D(player, partialTick);

            float screenX = (float) screenPos.x;
            float screenY = (float) screenPos.y;

            if (screenX < -64.0f || screenY < -64.0f || screenX > guiWidth + 64.0f || screenY > guiHeight + 64.0f)
                continue;

            String text = player.getName().getString();
            float scale = Math.max(0.65f, 1.0f - (dist / 256) * 0.35f);

            float maxHealth = player.getMaxHealth();
            float health = player.getHealth() + player.getAbsorptionAmount();
            String hpText = String.format("%.1f", health);
            Color hpColor = getHealthColor(maxHealth > 0.0f ? health / maxHealth : 0.0f);

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

            tags.add(new TagInfo(text, hpText, hpColor, items, screenX, screenY, scale));
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
