package com.github.lumin.gui.clickgui.component.impl;

import com.github.lumin.gui.Component;
import com.github.lumin.settings.impl.DoubleSetting;
import com.github.lumin.settings.impl.IntSetting;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberSettingComponent extends Component {
    private final Object setting;
    private final boolean isDouble;
    private boolean dragging;
    private boolean editing;
    private String editText = "";
    private final Animation sliderAnimation = new Animation(Easing.EASE_OUT_QUAD, 140L);
    private final Animation interactionAnimation = new Animation(Easing.EASE_OUT_QUAD, 120L);
    private final Animation editingAnimation = new Animation(Easing.EASE_OUT_QUAD, 120L);
    private float lastValueBoxX;
    private float lastValueBoxY;
    private float lastValueBoxW;
    private float lastValueBoxH;
    private float lastSliderX;
    private float lastSliderW;
    private float lastSliderHitY;
    private float lastSliderHitH;

    public NumberSettingComponent(IntSetting setting) {
        this.setting = setting;
        this.isDouble = false;
        float initial = getPercentInt(setting.getValue(), setting.getMin(), setting.getMax());
        sliderAnimation.setStartValue(initial);
        interactionAnimation.setStartValue(0.0f);
        editingAnimation.setStartValue(0.0f);
    }

    public NumberSettingComponent(DoubleSetting setting) {
        this.setting = setting;
        this.isDouble = true;
        float initial = getPercentDouble(setting.getValue(), setting.getMin(), setting.getMax());
        sliderAnimation.setStartValue(initial);
        interactionAnimation.setStartValue(0.0f);
        editingAnimation.setStartValue(0.0f);
    }

    public Object getSetting() {
        return setting;
    }

    public boolean isDragging() {
        return dragging;
    }

    private double getMin() {
        return isDouble ? ((DoubleSetting) setting).getMin() : ((IntSetting) setting).getMin();
    }

    private double getMax() {
        return isDouble ? ((DoubleSetting) setting).getMax() : ((IntSetting) setting).getMax();
    }

    private double getStep() {
        return isDouble ? ((DoubleSetting) setting).getStep() : ((IntSetting) setting).getStep();
    }

    private boolean isPercentageMode() {
        return isDouble ? ((DoubleSetting) setting).isPercentageMode() : ((IntSetting) setting).isPercentageMode();
    }

    private double getValue() {
        return isDouble ? ((DoubleSetting) setting).getValue() : ((IntSetting) setting).getValue();
    }

    private void setValue(double value) {
        if (isDouble) {
            ((DoubleSetting) setting).setValue(value);
        } else {
            ((IntSetting) setting).setValue((int) Math.round(value));
        }
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (setting instanceof IntSetting s && !s.isAvailable()) return;
        if (setting instanceof DoubleSetting s && !s.isAvailable()) return;

        boolean hovered = ColorSettingComponent.isMouseOutOfPicker(mouseX, mouseY) && MouseUtils.isHovering(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        Color bg = hovered ? new Color(255, 255, 255, (int) (18 * alpha)) : new Color(255, 255, 255, (int) (10 * alpha));
        set.bottomRoundRect().addRoundRect(getX(), getY(), getWidth(), getHeight(), 6.0f * scale, bg);

        float padding = 6.0f * scale;
        float textScale = 0.85f * scale;
        float textY = getY() + (getHeight() - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;

        String name = isDouble ? ((DoubleSetting) setting).getDisplayName() : ((IntSetting) setting).getDisplayName();
        set.font().addText(name, getX() + padding, textY, textScale, new Color(255, 255, 255, (int) (255 * alpha)));

        String valueStr;
        String valueMeasureStr;
        if (editing) {
            if (isPercentageMode()) {
                valueMeasureStr = editText + "%";
                valueStr = valueMeasureStr;
                if (System.currentTimeMillis() % 1000 > 500) valueStr += "_";
            } else {
                valueMeasureStr = editText;
                valueStr = editText;
                if (System.currentTimeMillis() % 1000 > 500) valueStr += "_";
            }
        } else if (isPercentageMode()) {
            double min = getMin();
            double max = getMax();
            double v = getValue();
            int percent = 0;
            if (max != min) {
                percent = (int) Math.round(((v - min) / (max - min)) * 100.0);
                percent = Mth.clamp(percent, 0, 100);
            }
            valueStr = percent + "%";
            valueMeasureStr = valueStr;
        } else {
            valueStr = formatValue(getValue());
            valueMeasureStr = valueStr;
        }

        float valueInnerPad = 4.0f * scale;
        String minStr = formatValue(getMin());
        String maxStr = formatValue(getMax());
        float valueMinW = set.font().getWidth(minStr, textScale);
        float valueMaxW = set.font().getWidth(maxStr, textScale);
        float valuePercentW = set.font().getWidth("100%", textScale);
        float valueBoxW = Math.max(valueMinW, Math.max(valueMaxW, valuePercentW)) + valueInnerPad * 6.0f + 8.0f * scale;
        float valueBoxH = Math.max(0.0f, getHeight() - 4.0f * scale);
        float valueBoxX = getX() + getWidth() - padding - valueBoxW;
        float valueBoxY = getY() + (getHeight() - valueBoxH) / 2.0f;
        lastValueBoxX = valueBoxX;
        lastValueBoxY = valueBoxY;
        lastValueBoxW = valueBoxW;
        lastValueBoxH = valueBoxH;

        editingAnimation.run(editing ? 1.0f : 0.0f);
        float et = Mth.clamp(editingAnimation.getValue(), 0.0f, 1.0f);
        int valueAlpha = (int) Mth.lerp(et, 12.0f, 22.0f);
        Color valueBg = new Color(255, 255, 255, (int) (Mth.clamp(valueAlpha, 0, 255) * alpha));
        set.bottomRoundRect().addRoundRect(valueBoxX, valueBoxY, valueBoxW, valueBoxH, 6.0f * scale, valueBg);

        float valueW = Mth.clamp(set.font().getWidth(valueMeasureStr, textScale), 0.0f, Math.max(0.0f, valueBoxW - valueInnerPad * 2.0f));
        float valueX = valueBoxX + (valueBoxW - valueW) / 2.0f;
        float valueTextY = valueBoxY + (valueBoxH - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;
        set.font().addText(valueStr, valueX, valueTextY, textScale, new Color(200, 200, 200, (int) (255 * alpha)));

        float sliderWidth = 70.0f * scale;
        float sliderHeight = 3.0f * scale;
        float sliderX = valueBoxX - padding - sliderWidth;
        float sliderY = getY() + (getHeight() - sliderHeight) / 2.0f;
        lastSliderX = sliderX;
        lastSliderW = sliderWidth;
        lastSliderHitH = Math.max(12.0f * scale, sliderHeight);
        lastSliderHitY = getY() + (getHeight() - lastSliderHitH) / 2.0f;

        boolean sliderHovered = !editing && MouseUtils.isHovering(lastSliderX, lastSliderHitY, lastSliderW, lastSliderHitH, mouseX, mouseY);
        float interactionTarget = dragging ? 1.0f : (sliderHovered ? 0.6f : 0.0f);
        interactionAnimation.run(interactionTarget);
        float it = Mth.clamp(interactionAnimation.getValue(), 0.0f, 1.0f);

        if (!editing && dragging) {
            float mouseRelX = mouseX - sliderX;
            float percent = Mth.clamp(mouseRelX / sliderWidth, 0.0f, 1.0f);
            double range = getMax() - getMin();
            double newVal = getMin() + (range * percent);

            double step = getStep();
            if (step > 0) {
                double stepped = Math.round((newVal - getMin()) / step) * step;
                newVal = getMin() + stepped;
            }
            setValue(newVal);
        }

        int track = (int) Mth.lerp(it, 60.0f, 82.0f);
        set.bottomRoundRect().addRoundRect(sliderX, sliderY, sliderWidth, sliderHeight, sliderHeight / 2.0f, new Color(track, track, track, (int) (255 * alpha)));

        float targetPercent = (float) ((getValue() - getMin()) / (getMax() - getMin()));
        if (!editing && dragging) {
            sliderAnimation.setStartValue(targetPercent);
        } else {
            sliderAnimation.run(targetPercent);
        }
        float animatedPercent = Mth.clamp(sliderAnimation.getValue(), 0.0f, 1.0f);

        float filledW = Mth.clamp(sliderWidth * animatedPercent, 0.0f, sliderWidth);

        if (filledW > 0) {
            int fill = (int) Mth.lerp(it, 148.0f, 176.0f);
            set.bottomRoundRect().addRoundRect(sliderX, sliderY, filledW, sliderHeight, sliderHeight / 2.0f, new Color(fill, fill, fill, (int) (255 * alpha)));
        }

        float knobSize = 8.0f * scale * (1.0f + 0.35f * it);
        float knobX = sliderX + filledW - knobSize / 2.0f;
        float knobY = getY() + (getHeight() - knobSize) / 2.0f;
        float glowSize = knobSize + 4.0f * scale;
        float glowX = (sliderX + filledW) - glowSize / 2.0f;
        float glowY = getY() + (getHeight() - glowSize) / 2.0f;
        int glowA = (int) (42.0f * it);
        if (glowA > 0) {
            set.bottomRoundRect().addRoundRect(glowX, glowY, glowSize, glowSize, glowSize / 2.0f, new Color(255, 255, 255, (int) (Mth.clamp(glowA, 0, 255) * alpha)));
        }
        set.bottomRoundRect().addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, new Color(255, 255, 255, (int) (255 * alpha)));
    }

    private static float getPercentInt(int value, int min, int max) {
        if (max == min) return 0.0f;
        return (float) (value - min) / (float) (max - min);
    }

    private static float getPercentDouble(double value, double min, double max) {
        if (max == min) return 0.0f;
        return (float) ((value - min) / (max - min));
    }

    private int getDecimalPlaces() {
        double step = getStep();
        if (step <= 0.0) return isDouble ? 2 : 0;
        BigDecimal bd = BigDecimal.valueOf(step).stripTrailingZeros();
        return Mth.clamp(bd.scale(), 0, 8);
    }

    private String formatValue(double value) {
        if (!isDouble) {
            return String.valueOf((int) Math.round(value));
        }
        int decimals = getDecimalPlaces();
        BigDecimal bd = BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros();
        String s = bd.toPlainString();
        return s.equals("-0") ? "0" : s;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (editing && !MouseUtils.isHovering(lastValueBoxX, lastValueBoxY, lastValueBoxW, lastValueBoxH, event.x(), event.y())) {
            applyEditText();
            editing = false;
            dragging = false;
            return true;
        }

        if (event.button() == 0) {
            if (MouseUtils.isHovering(lastSliderX, lastSliderHitY, lastSliderW, lastSliderHitH, event.x(), event.y())) {
                if (editing) return true;
                dragging = true;

                float mouseRelX = (float) event.x() - lastSliderX;
                float percent = Mth.clamp(mouseRelX / lastSliderW, 0.0f, 1.0f);
                double range = getMax() - getMin();
                double newVal = getMin() + (range * percent);

                double step = getStep();
                if (step > 0) {
                    double stepped = Math.round((newVal - getMin()) / step) * step;
                    newVal = getMin() + stepped;
                }
                setValue(newVal);
                return true;
            }
        }
        if (event.button() == 1) {
            if (MouseUtils.isHovering(lastValueBoxX, lastValueBoxY, lastValueBoxW, lastValueBoxH, event.x(), event.y())) {
                dragging = false;
                editing = true;
                if (isPercentageMode()) {
                    double min = getMin();
                    double max = getMax();
                    double v = getValue();
                    int percent = 0;
                    if (max != min) {
                        percent = (int) Math.round(((v - min) / (max - min)) * 100.0);
                        percent = Mth.clamp(percent, 0, 100);
                    }
                    editText = String.valueOf(percent);
                } else {
                    editText = formatValue(getValue());
                }
                return true;
            }
            if (editing) {
                applyEditText();
                editing = false;
                return true;
            }
        }
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!editing) return super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!editText.isEmpty()) {
                editText = editText.substring(0, editText.length() - 1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            applyEditText();
            editing = false;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            editing = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!editing) return super.charTyped(input);
        char c = (char) input.codepoint();
        if (Character.isDigit(c)) {
            editText += c;
            return true;
        }
        if (c == '-' && editText.isEmpty()) {
            editText = "-";
            return true;
        }
        if (isDouble && c == '.' && !editText.contains(".")) {
            editText += ".";
            return true;
        }
        if (c == '%' && isPercentageMode()) {
            return true;
        }
        return super.charTyped(input);
    }

    private void applyEditText() {
        String raw = editText == null ? "" : editText.trim();
        if (raw.isEmpty() || raw.equals("-") || (isDouble && raw.equals("."))) return;
        try {
            if (isPercentageMode()) {
                raw = raw.replace("%", "");
                double p = Double.parseDouble(raw);
                p = Mth.clamp(p, 0.0, 100.0);
                double min = getMin();
                double max = getMax();
                double newVal = min;
                if (max != min) {
                    newVal = min + ((max - min) * (p / 100.0));
                }
                double step = getStep();
                if (step > 0) {
                    double stepped = Math.round((newVal - min) / step) * step;
                    newVal = min + stepped;
                }
                setValue(newVal);
            } else {
                double newVal = Double.parseDouble(raw);
                double step = getStep();
                if (step > 0) {
                    double stepped = Math.round((newVal - getMin()) / step) * step;
                    newVal = getMin() + stepped;
                }
                setValue(newVal);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
