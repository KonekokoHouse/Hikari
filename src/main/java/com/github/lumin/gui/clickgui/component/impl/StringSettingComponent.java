package com.github.lumin.gui.clickgui.component.impl;

import com.github.lumin.gui.Component;
import com.github.lumin.settings.impl.StringSetting;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class StringSettingComponent extends Component {
    private final StringSetting setting;
    private boolean focused;
    private final Animation focusAnimation = new Animation(Easing.EASE_OUT_QUAD, 140L);

    public StringSettingComponent(StringSetting setting) {
        this.setting = setting;
        focusAnimation.setStartValue(0.0f);
    }

    public StringSetting getSetting() {
        return setting;
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (!setting.isAvailable()) return;

        boolean hovered = ColorSettingComponent.isMouseOutOfPicker(mouseX, mouseY) && MouseUtils.isHovering(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        float target = focused ? 1.0f : (hovered ? 0.6f : 0.0f);
        focusAnimation.run(target);
        float t = Mth.clamp(focusAnimation.getValue(), 0.0f, 1.0f);
        int bgAlpha = (int) Mth.lerp(t, 10.0f, 22.0f);
        Color bg = new Color(255, 255, 255, (int) (Mth.clamp(bgAlpha, 0, 255) * alpha));
        set.bottomRoundRect().addRoundRect(getX(), getY(), getWidth(), getHeight(), 6.0f * scale, bg);

        String name = setting.getDisplayName();
        float textScale = 0.85f * scale;
        float textY = getY() + (getHeight() - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;
        set.font().addText(name, getX() + 6.0f * scale, textY, textScale, new Color(255, 255, 255, (int) (255 * alpha)));

        String value = setting.getValue() == null ? "" : setting.getValue();
        if (focused && (System.currentTimeMillis() % 1000 > 500)) {
            value += "_";
        }
        float maxValueW = Math.max(0.0f, getWidth() * 0.55f);
        float valueW = Math.min(set.font().getWidth(value, textScale), maxValueW);
        float valueX = getX() + getWidth() - 6.0f * scale - valueW;
        set.font().addText(value, valueX, textY, textScale, new Color(200, 200, 200, (int) (255 * alpha)));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (event.button() == 0) {
            this.focused = isHovered((float) event.x(), (float) event.y());
        }
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!focused) return super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            String v = setting.getValue();
            if (v != null && !v.isEmpty()) {
                setting.setValue(v.substring(0, v.length() - 1));
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_ESCAPE) {
            focused = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!focused) return super.charTyped(input);
        String v = setting.getValue();
        if (v == null) v = "";
        setting.setValue(v + Character.toString(input.codepoint()));
        return true;
    }

}
