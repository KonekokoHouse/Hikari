package com.github.lumin.gui.clickgui.component.impl;

import com.github.lumin.gui.Component;
import com.github.lumin.settings.impl.BoolSetting;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.awt.*;

public class BoolSettingComponent extends Component {
    private final BoolSetting setting;
    private final Animation toggleAnimation = new Animation(Easing.EASE_OUT_QUAD, 120L);

    public BoolSettingComponent(BoolSetting setting) {
        this.setting = setting;
        toggleAnimation.setStartValue(setting.getValue() ? 1.0f : 0.0f);
    }

    public BoolSetting getSetting() {
        return setting;
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (!setting.isAvailable()) return;

        boolean hovered = ColorSettingComponent.isMouseOutOfPicker(mouseX, mouseY) && MouseUtils.isHovering(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        Color bg = hovered ? new Color(255, 255, 255, (int) (18 * alpha)) : new Color(255, 255, 255, (int) (10 * alpha));
        set.bottomRoundRect().addRoundRect(getX(), getY(), getWidth(), getHeight(), 6.0f * scale, bg);

        String name = setting.getDisplayName();

        float textScale = 0.85f * scale;
        float textY = getY() + (getHeight() - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;
        set.font().addText(name, getX() + 6.0f * scale, textY, textScale, new Color(255, 255, 255, (int) (255 * alpha)));

        float switchW = 22.0f * scale;
        float switchH = 10.0f * scale;
        float switchX = getX() + getWidth() - 6.0f * scale - switchW;
        float switchY = getY() + (getHeight() - switchH) / 2.0f;

        float target = setting.getValue() ? 1.0f : 0.0f;
        toggleAnimation.run(target);
        float t = toggleAnimation.getValue();
        if (t < 0.0f) t = 0.0f;
        if (t > 1.0f) t = 1.0f;

        Color trackColor = lerpColor(new Color(60, 60, 60), new Color(148, 148, 148), t);
        set.bottomRoundRect().addRoundRect(switchX, switchY, switchW, switchH, switchH / 2.0f, new Color(trackColor.getRed(), trackColor.getGreen(), trackColor.getBlue(), (int) (255 * alpha)));

        float thumbSize = switchH - 2.0f * scale;
        float offX = switchX + 1.0f * scale;
        float onX = switchX + switchW - thumbSize - 1.0f * scale;
        float thumbX = offX + (onX - offX) * t;
        float thumbY = switchY + 1.0f * scale;

        set.bottomRoundRect().addRoundRect(thumbX, thumbY, thumbSize, thumbSize, thumbSize / 2.0f, new Color(255, 255, 255, (int) (255 * alpha)));
    }

    private static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
        return new Color(Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), Mth.clamp(bl, 0, 255), Mth.clamp(al, 0, 255));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (event.button() == 0 && isHovered((float) event.x(), (float) event.y())) {
            setting.setValue(!setting.getValue());
            return true;
        }
        return super.mouseClicked(event, focused);
    }
}
