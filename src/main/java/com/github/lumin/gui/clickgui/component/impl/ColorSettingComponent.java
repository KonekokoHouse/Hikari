package com.github.lumin.gui.clickgui.component.impl;

import com.github.lumin.gui.Component;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.settings.impl.ColorSetting;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class ColorSettingComponent extends Component {

    private final ColorPicker colorPicker = new ColorPicker();
    private static ColorSettingComponent activePicker = null;

    private final ColorSetting setting;
    private boolean opened;
    private final Animation openAnimation = new Animation(Easing.EASE_OUT_QUAD, 120L);
    private float openMouseX;
    private float openMouseY;
    private float lastSwatchX;
    private float lastSwatchY;
    private float lastSwatchW;
    private float lastSwatchH;

    public ColorSettingComponent(ColorSetting setting) {
        this.setting = setting;
        openAnimation.setStartValue(0.0f);
    }

    public ColorSetting getSetting() {
        return setting;
    }

    public static boolean isMouseOutOfPicker(int mouseX, int mouseY) {
        return activePicker == null || !activePicker.colorPicker.isHovering(mouseX, mouseY);
    }

    public static boolean hasActivePicker() {
        return activePicker != null;
    }

    public static void closeActivePicker() {
        if (activePicker != null) {
            activePicker.closePicker();
        }
    }

    private void closePicker() {
        if (opened) {
            colorPicker.applyEditing(setting);
            opened = false;
        }
        if (activePicker == this) {
            activePicker = null;
        }
        openAnimation.setStartValue(0.0f);
        colorPicker.cancelEditing();
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (!setting.isAvailable()) return;

        boolean hovered = isMouseOutOfPicker(mouseX, mouseY) && MouseUtils.isHovering(getX(), getY(), getWidth(), getHeight(), mouseX, mouseY);
        Color bg = hovered ? new Color(255, 255, 255, (int) (18 * alpha)) : new Color(255, 255, 255, (int) (10 * alpha));
        set.bottomRoundRect().addRoundRect(getX(), getY(), getWidth(), getHeight(), 6.0f * scale, bg);

        String name = setting.getDisplayName();
        Color value = setting.getValue();
        String hex = value == null ? "null" : String.format("#%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue());

        float textScale = 0.85f * scale;
        float textY = getY() + (getHeight() - set.font().getHeight(textScale)) / 2.0f - 0.5f * scale;
        set.font().addText(name, getX() + 6.0f * scale, textY, textScale, new Color(255, 255, 255, (int) (255 * alpha)));

        float sw = 10.0f * scale;
        float sx = getX() + getWidth() - 6.0f * scale - sw;
        float sy = getY() + (getHeight() - sw) / 2.0f;
        lastSwatchX = sx;
        lastSwatchY = sy;
        lastSwatchW = sw;
        lastSwatchH = sw;

        Color swatchColor = value == null ? new Color(128, 128, 128, (int) (255 * alpha)) : new Color(value.getRed(), value.getGreen(), value.getBlue(), (int) (255 * alpha));
        set.bottomRoundRect().addRoundRect(sx, sy, sw, sw, 3.0f * scale, swatchColor);

        float hexW = set.font().getWidth(hex, textScale);
        set.font().addText(hex, sx - 6.0f * scale - hexW, textY, textScale, new Color(200, 200, 200, (int) (255 * alpha)));
    }

    public boolean isOpened() {
        return opened;
    }

    public void renderOverlay(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        if (!opened) return;
        float panelW = ColorPicker.preferredWidth(scale);
        float panelH = ColorPicker.preferredHeight(scale, setting.isAllowAlpha());
        float panelX = lastSwatchX + lastSwatchW - panelW;
        float panelY = (lastSwatchY + lastSwatchH / 2.0f) - panelH / 2.0f;

        openAnimation.run(1.0f);
        float t = Mth.clamp(openAnimation.getValue(), 0.0f, 1.0f);

        float finalCX = panelX + panelW / 2.0f;
        float finalCY = panelY + panelH / 2.0f;
        float cx = Mth.lerp(t, openMouseX, finalCX);
        float cy = Mth.lerp(t, openMouseY, finalCY);
        float w = Math.max(1.0f, panelW * t);
        float h = Math.max(1.0f, panelH * t);
        float x = cx - w / 2.0f;
        float y = cy - h / 2.0f;
        float radius = 7.0f * scale * t;

        if (t < 0.98f) {
            colorPicker.x = x;
            colorPicker.y = y;
            colorPicker.width = w;
            colorPicker.height = h;
            set.pickingRound().addRoundRect(x, y, w, h, radius, new Color(0, 0, 0, 120));
            int outlineAlpha = (int) (10.0f * t);
            if (outlineAlpha > 0) {
                set.pickingRound().addRoundRect(x, y, w, h, radius, new Color(255, 255, 255, outlineAlpha));
            }
            return;
        }

        colorPicker.render(set, panelX, panelY, panelW, mouseX, mouseY, scale, setting);
    }

    public void renderOverlayBlur(int mouseX, int mouseY, float partialTicks) {
        if (!opened) return;
        if (!ClickGui.INSTANCE.backgroundBlur.getValue()) return;
        float panelW = ColorPicker.preferredWidth(scale);
        float panelH = ColorPicker.preferredHeight(scale, setting.isAllowAlpha());
        float panelX = lastSwatchX + lastSwatchW - panelW;
        float panelY = (lastSwatchY + lastSwatchH / 2.0f) - panelH / 2.0f;
        float t = Mth.clamp(openAnimation.getValue(), 0.0f, 1.0f);
        if (t <= 0.0f) return;

        float finalCX = panelX + panelW / 2.0f;
        float finalCY = panelY + panelH / 2.0f;
        float cx = Mth.lerp(t, openMouseX, finalCX);
        float cy = Mth.lerp(t, openMouseY, finalCY);
        float w = Math.max(1.0f, panelW * t);
        float h = Math.max(1.0f, panelH * t);
        float x = cx - w / 2.0f;
        float y = cy - h / 2.0f;
        float radius = 7.0f * scale * t;
        //drawRoundedBlur(x, y, w, h, radius, ClickGui.INSTANCE.blurStrength.getValue().floatValue() * t);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (!setting.isAvailable()) return super.mouseClicked(event, focused);

        // 如果有活动的 Picker 且鼠标悬停在它的面板上，拦截其他组件的点击
        if (activePicker != null && activePicker != this && activePicker.colorPicker.isHovering(event.x(), event.y())) {
            return false;
        }

        if (event.button() == 1 && MouseUtils.isHovering(lastSwatchX, lastSwatchY, lastSwatchW, lastSwatchH, event.x(), event.y())) {
            if (opened) {
                closePicker();
            } else {
                if (activePicker != null) {
                    activePicker.closePicker();
                }
                opened = true;
                colorPicker.syncFromSetting(setting);
                activePicker = this;
                openMouseX = (float) event.x();
                openMouseY = (float) event.y();
                openAnimation.setStartValue(0.0f);
                openAnimation.reset();
            }
            return true;
        }

        if (opened) {
            if (colorPicker.mouseClicked(event, setting)) return true;
            if (colorPicker.isHovering(event.x(), event.y())) {
                return true;
            }
            closePicker();
            return false;
        }

        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (opened) {
            colorPicker.mouseReleased();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (opened && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            closePicker();
            return true;
        }
        if (opened && colorPicker.keyPressed(event, setting)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (opened && colorPicker.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private static class ColorPicker {
        private static final int CHANNEL_NONE = -1;
        private static final int CHANNEL_R = 0;
        private static final int CHANNEL_G = 1;
        private static final int CHANNEL_B = 2;
        private static final int CHANNEL_A = 3;

        private float x;
        private float y;
        private float width;
        private float height;

        private float pickerX;
        private float pickerY;
        private float pickerS;

        private float hueX;
        private float hueY;
        private float hueW;
        private float hueH;

        private float hue;
        private float sat;
        private float bri;
        private int alpha;

        private boolean draggingPicker;
        private boolean draggingHue;
        private int draggingChannel = CHANNEL_NONE;

        private int editingChannel = CHANNEL_NONE;
        private String editText = "";

        private final float[] channelSliderX = new float[4];
        private final float[] channelSliderY = new float[4];
        private final float[] channelSliderW = new float[4];
        private final float[] channelSliderH = new float[4];
        private final float[] channelBoxX = new float[4];
        private final float[] channelBoxY = new float[4];
        private final float[] channelBoxW = new float[4];
        private final float[] channelBoxH = new float[4];

        public static float preferredHeight(float scale, boolean allowAlpha) {
            int channelCount = allowAlpha ? 4 : 3;
            float pad = 7.0f * scale;
            float pickerSize = 96.0f * scale;
            float rowH = 14.0f * scale;
            float rowGap = 5.0f * scale;
            return pickerSize + 3.0f * pad + channelCount * rowH + Math.max(0, channelCount - 1) * rowGap;
        }

        public static float preferredWidth(float scale) {
            float pad = 7.0f * scale;
            float pickerSize = 96.0f * scale;
            float hueW = 9.0f * scale;
            float gap = 5.0f * scale;
            return pickerSize + gap + hueW + pad * 2.0f;
        }

        public void syncFromSetting(ColorSetting setting) {
            Color c = setting.getValue();
            if (c == null) c = new Color(255, 255, 255, 255);

            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            hue = hsb[0];
            sat = hsb[1];
            bri = hsb[2];
            alpha = setting.isAllowAlpha() ? c.getAlpha() : 255;
        }

        public void cancelEditing() {
            editingChannel = CHANNEL_NONE;
            editText = "";
            draggingPicker = false;
            draggingHue = false;
            draggingChannel = CHANNEL_NONE;
        }

        public void applyEditing(ColorSetting setting) {
            if (editingChannel == CHANNEL_NONE) return;
            applyEditText(setting);
            editingChannel = CHANNEL_NONE;
            editText = "";
        }

        public boolean isHovering(double mouseX, double mouseY) {
            return MouseUtils.isHovering(x, y, width, height, mouseX, mouseY);
        }

        public void render(RendererSet set, float x, float y, float w, int mouseX, int mouseY, float scale, ColorSetting setting) {
            this.x = x;
            this.y = y;
            this.width = w;

            float pad = 7.0f * scale;
            float gap = 5.0f * scale;
            float radius = 7.0f * scale;

            float pickerSize = 96.0f * scale;
            pickerX = x + pad;
            pickerY = y + pad;
            pickerS = pickerSize;

            hueW = 9.0f * scale;
            hueH = pickerSize;
            hueX = pickerX + pickerSize + gap;
            hueY = pickerY;

            boolean allowAlpha = setting.isAllowAlpha();
            int channelCount = allowAlpha ? 4 : 3;
            float rowH = 14.0f * scale;
            float rowGap = 5.0f * scale;
            float rowsTop = pickerY + pickerSize + pad;

            this.height = (rowsTop - y) + channelCount * rowH + Math.max(0, channelCount - 1) * rowGap + pad;

            set.pickingRound().addRoundRect(x, y, w, height, radius, new Color(0, 0, 0, 120));

            int[] rgba = getRgba(setting);
            int r = rgba[0];
            int g = rgba[1];
            int b = rgba[2];
            int a = rgba[3];

            if (draggingPicker) {
                float px = (float) Mth.clamp((mouseX - pickerX) / pickerSize, 0.0, 1.0);
                float py = (float) Mth.clamp((mouseY - pickerY) / pickerSize, 0.0, 1.0);
                sat = px;
                bri = 1.0f - py;
                int[] rgb = hsbToRgb(hue, sat, bri);
                applyColor(setting, rgb[0], rgb[1], rgb[2], a);
                r = rgb[0];
                g = rgb[1];
                b = rgb[2];
            }

            if (draggingHue) {
                float py = (float) Mth.clamp((mouseY - hueY) / hueH, 0.0, 1.0);
                hue = py;
                int[] rgb = hsbToRgb(hue, sat, bri);
                applyColor(setting, rgb[0], rgb[1], rgb[2], a);
                r = rgb[0];
                g = rgb[1];
                b = rgb[2];
            }

            if (draggingChannel != CHANNEL_NONE) {
                int channel = draggingChannel;
                float sx = channelSliderX[channel];
                float sw = channelSliderW[channel];
                float percent = (float) Mth.clamp((mouseX - sx) / sw, 0.0, 1.0);
                int v = Math.round(percent * 255.0f);

                if (channel == CHANNEL_R) r = v;
                if (channel == CHANNEL_G) g = v;
                if (channel == CHANNEL_B) b = v;
                if (channel == CHANNEL_A) a = v;

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                hue = hsb[0];
                sat = hsb[1];
                bri = hsb[2];
                applyColor(setting, r, g, b, a);
            }

            Color hueColor = Color.getHSBColor(hue, 1.0f, 1.0f);
            drawPicker(set, pickerX, pickerY, pickerSize, hueColor, scale);
            drawHueBar(set, hueX, hueY, hueW, hueH, scale);

            float indicatorR = 4.0f * scale;
            float indX = pickerX + sat * pickerSize;
            float indY = pickerY + (1.0f - bri) * pickerSize;
            set.pickerRound().addRoundRect(indX - indicatorR - 1.0f * scale, indY - indicatorR - 1.0f * scale, (indicatorR + 1.0f * scale) * 2.0f, (indicatorR + 1.0f * scale) * 2.0f, (indicatorR + 1.0f * scale), new Color(0, 0, 0, 160));
            set.pickerRound().addRoundRect(indX - indicatorR, indY - indicatorR, indicatorR * 2.0f, indicatorR * 2.0f, indicatorR, new Color(255, 255, 255, 220));

            float hueIndY = hueY + hue * hueH;
            set.pickerRound().addRoundRect(hueX - 2.0f * scale, hueIndY - 2.0f * scale, hueW + 4.0f * scale, 4.0f * scale, 2.0f * scale, new Color(255, 255, 255, 200));

            float textScale = 0.80f * scale;
            float boxInnerPad = 4.0f * scale;
            float valueBoxW = set.font().getWidth("255", textScale) + boxInnerPad * 2.0f + 10.0f * scale;
            float sliderH = 3.0f * scale;
            float hitH = Math.max(12.0f * scale, sliderH);
            float knobSize = 8.0f * scale;

            for (int i = 0; i < channelCount; i++) {
                float rowY = rowsTop + i * (rowH + rowGap);
                float rowX = x + pad;
                // 让 Slider 行的总宽度与上方的 Picker 区域对齐
                // Picker 区域总宽 = pickerSize + gap + hueW
                // 因此 rowW 也设为这个宽度
                float rowW = w - pad * 2.0f;

                float labelW = set.pickingText().getWidth(channelName(i), textScale);
                set.pickingText().addText(channelName(i), rowX, rowY + (rowH - set.pickingText().getHeight(textScale)) / 2.0f - 0.5f * scale, textScale, Color.WHITE);

                // boxX 的计算基于 rowX + rowW，也就是右对齐到 Picker 区域的右边缘
                float boxX = rowX + rowW - valueBoxW;
                float boxY = rowY + (rowH - (rowH - 2.0f * scale)) / 2.0f;
                float boxH = rowH - 2.0f * scale;

                channelBoxX[i] = boxX;
                channelBoxY[i] = boxY;
                channelBoxW[i] = valueBoxW;
                channelBoxH[i] = boxH;

                float sliderX = rowX + labelW + gap;
                float sliderW = boxX - gap - sliderX;
                float sliderY = rowY + (rowH - sliderH) / 2.0f;
                float sliderHitY = rowY + (rowH - hitH) / 2.0f;

                channelSliderX[i] = sliderX;
                channelSliderY[i] = sliderHitY;
                channelSliderW[i] = Math.max(1.0f, sliderW);
                channelSliderH[i] = hitH;

                set.pickingRound().addRoundRect(sliderX, sliderY, sliderW, sliderH, sliderH / 2.0f, new Color(60, 60, 60));

                int currentV = channelValue(i, r, g, b, a);
                float filledW = sliderW * (currentV / 255.0f);
                filledW = (float) Mth.clamp(filledW, 0.0, sliderW);

                if (filledW > 0.0f) {
                    Color left = channelColorMin(i, r, g, b, a);
                    Color right = channelColorMax(i, r, g, b, a);
                    Color currentC = lerp(left, right, currentV / 255.0f);

                    if (filledW > sliderH) {
                        // 左端圆头
                        set.pickingRound().addRoundRect(sliderX, sliderY, sliderH, sliderH, sliderH / 2.0f, left);

                        // 中间渐变段
                        set.pickingRect().addHorizontalGradient(sliderX + sliderH / 2.0f, sliderY, filledW - sliderH, sliderH, left, currentC);

                        // 右端圆头
                        set.pickerRound().addRoundRect(sliderX + filledW - sliderH, sliderY, sliderH, sliderH, sliderH / 2.0f, currentC);
                    } else {
                        // 进度很短时，直接画一个圆角矩形
                        set.pickerRound().addRoundRect(sliderX, sliderY, filledW, sliderH, sliderH / 2.0f, left);
                    }
                }

                float knobX = sliderX + filledW - knobSize / 2.0f;
                float knobY = rowY + (rowH - knobSize) / 2.0f;
                set.pickerRound().addRoundRect(knobX, knobY, knobSize, knobSize, knobSize / 2.0f, Color.WHITE);

                boolean editing = editingChannel == i;
                Color boxBg = editing ? new Color(255, 255, 255, 22) : new Color(255, 255, 255, 12);
                set.pickerRound().addRoundRect(boxX, boxY, valueBoxW, boxH, 4.5f * scale, boxBg);

                String valueStr = editing ? editText : String.valueOf(currentV);
                String valueMeasureStr = editing ? editText : String.valueOf(currentV);
                if (editing && (System.currentTimeMillis() % 1000 > 500)) {
                    valueStr = valueStr + "_";
                }
                float valueW = Math.min(set.pickingText().getWidth(valueMeasureStr, textScale), Math.max(0.0f, valueBoxW - boxInnerPad * 2.0f));
                float valueX = boxX + (valueBoxW - valueW) / 2.0f;
                float valueTextY = boxY + (boxH - set.pickingText().getHeight(textScale)) / 2.0f - 0.5f * scale;
                set.pickingText().addText(valueStr, valueX, valueTextY, textScale, new Color(200, 200, 200));
            }
        }

        public boolean mouseClicked(MouseButtonEvent event, ColorSetting setting) {
            if (event.button() == 0) {
                if (MouseUtils.isHovering(pickerX, pickerY, pickerS, pickerS, event.x(), event.y())) {
                    applyEditing(setting);
                    draggingPicker = true;
                    return true;
                }
                if (MouseUtils.isHovering(hueX, hueY, hueW, hueH, event.x(), event.y())) {
                    applyEditing(setting);
                    draggingHue = true;
                    return true;
                }

                boolean allowAlpha = setting.isAllowAlpha();
                int channelCount = allowAlpha ? 4 : 3;
                for (int i = 0; i < channelCount; i++) {
                    if (MouseUtils.isHovering(channelSliderX[i], channelSliderY[i], channelSliderW[i], channelSliderH[i], event.x(), event.y())) {
                        if (editingChannel != CHANNEL_NONE) return true;
                        draggingChannel = i;
                        return true;
                    }
                }

                if (editingChannel != CHANNEL_NONE && !MouseUtils.isHovering(channelBoxX[editingChannel], channelBoxY[editingChannel], channelBoxW[editingChannel], channelBoxH[editingChannel], event.x(), event.y())) {
                    applyEditing(setting);
                    return true;
                }
            }

            if (event.button() == 1) {
                boolean allowAlpha = setting.isAllowAlpha();
                int channelCount = allowAlpha ? 4 : 3;
                for (int i = 0; i < channelCount; i++) {
                    if (MouseUtils.isHovering(channelBoxX[i], channelBoxY[i], channelBoxW[i], channelBoxH[i], event.x(), event.y())) {
                        draggingPicker = false;
                        draggingHue = false;
                        draggingChannel = CHANNEL_NONE;
                        editingChannel = i;
                        editText = String.valueOf(channelValue(i, getRgba(setting)[0], getRgba(setting)[1], getRgba(setting)[2], getRgba(setting)[3]));
                        return true;
                    }
                }
                if (editingChannel != CHANNEL_NONE) {
                    applyEditing(setting);
                    return true;
                }
            }

            return false;
        }

        public void mouseReleased() {
            draggingPicker = false;
            draggingHue = false;
            draggingChannel = CHANNEL_NONE;
        }

        public boolean keyPressed(KeyEvent event, ColorSetting setting) {
            if (editingChannel == CHANNEL_NONE) return false;
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editText.isEmpty()) {
                    editText = editText.substring(0, editText.length() - 1);
                }
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER) {
                applyEditing(setting);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                editingChannel = CHANNEL_NONE;
                editText = "";
                return true;
            }
            return false;
        }

        public boolean charTyped(CharacterEvent event) {
            if (editingChannel == CHANNEL_NONE) return false;
            char c = (char) event.codepoint();
            if (Character.isDigit(c)) {
                editText += c;
                if (editText.length() > 3) {
                    editText = editText.substring(0, 3);
                }
                return true;
            }
            return false;
        }

        private void applyEditText(ColorSetting setting) {
            String raw = editText == null ? "" : editText.trim();
            if (raw.isEmpty()) return;
            try {
                int v = Integer.parseInt(raw);
                v = Mth.clamp(v, 0, 255);

                int[] rgba = getRgba(setting);
                int r = rgba[0];
                int g = rgba[1];
                int b = rgba[2];
                int a = rgba[3];

                if (editingChannel == CHANNEL_R) r = v;
                if (editingChannel == CHANNEL_G) g = v;
                if (editingChannel == CHANNEL_B) b = v;
                if (editingChannel == CHANNEL_A) a = v;

                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                hue = hsb[0];
                sat = hsb[1];
                bri = hsb[2];
                applyColor(setting, r, g, b, a);
            } catch (NumberFormatException ignored) {
            }
        }

        private static int[] getRgba(ColorSetting setting) {
            Color c = setting.getValue();
            if (c == null) c = new Color(255, 255, 255, 255);
            int a = setting.isAllowAlpha() ? c.getAlpha() : 255;
            return new int[]{c.getRed(), c.getGreen(), c.getBlue(), a};
        }

        private static void applyColor(ColorSetting setting, int r, int g, int b, int a) {
            if (!setting.isAllowAlpha()) a = 255;
            setting.setValue(new Color(Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), Mth.clamp(b, 0, 255), Mth.clamp(a, 0, 255)));
        }

        private static int[] hsbToRgb(float h, float s, float b) {
            int rgb = Color.HSBtoRGB(Mth.clamp(h, 0.0f, 1.0f), Mth.clamp(s, 0.0f, 1.0f), Mth.clamp(b, 0.0f, 1.0f));
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int bl = rgb & 0xFF;
            return new int[]{r, g, bl};
        }

        private static void drawPicker(RendererSet set, float x, float y, float size, Color hueColor, float scale) {
            // 第一层：水平渐变，左白 -> 右纯色 (饱和度控制)
            set.pickingRect().addHorizontalGradient(x, y, size, size, Color.WHITE, hueColor);

            // 第二层：垂直渐变，上透明 -> 下黑色 (亮度控制)
            set.pickingRect().addVerticalGradient(x, y, size, size, new Color(0, 0, 0, 0), Color.BLACK);
        }

        private static void drawHueBar(RendererSet set, float x, float y, float w, float h, float scale) {
            // 色相条由多个垂直渐变段组成，形成平滑的彩虹色
            // 色相环: 红 -> 黄 -> 绿 -> 青 -> 蓝 -> 洋红 -> 红
            // 0 -> 60 -> 120 -> 180 -> 240 -> 300 -> 360

            int steps = 6; // 6个主色相区间
            float segmentH = h / steps;

            // 定义主色相点
            // HSB(0, 1, 1) = Red
            // HSB(1/6, 1, 1) = Yellow
            // ...

            for (int i = 0; i < steps; i++) {
                float startHue = (float) i / steps;
                float endHue = (float) (i + 1) / steps;

                Color c1 = Color.getHSBColor(startHue, 1.0f, 1.0f);
                Color c2 = Color.getHSBColor(endHue, 1.0f, 1.0f);

                float segY = y + i * segmentH;
                // 为了防止缝隙，可以稍微延伸一点点高度，或者就用精确浮点
                // 这里直接用 segmentH

                set.pickingRect().addVerticalGradient(x, segY, w, segmentH, c1, c2);
            }

            // 绘制边框
            set.pickingRound().addRoundRect(x - 1.0f * scale, y - 1.0f * scale, w + 2.0f * scale, h + 2.0f * scale, 6.0f * scale, new Color(255, 255, 255, 18));
        }

        private static String channelName(int channel) {
            if (channel == CHANNEL_R) return "R";
            if (channel == CHANNEL_G) return "G";
            if (channel == CHANNEL_B) return "B";
            return "A";
        }

        private static int channelValue(int channel, int r, int g, int b, int a) {
            if (channel == CHANNEL_R) return r;
            if (channel == CHANNEL_G) return g;
            if (channel == CHANNEL_B) return b;
            return a;
        }

        private static Color channelColorMin(int channel, int r, int g, int b, int a) {
            if (channel == CHANNEL_R) return new Color(0, g, b, a);
            if (channel == CHANNEL_G) return new Color(r, 0, b, a);
            if (channel == CHANNEL_B) return new Color(r, g, 0, a);
            return new Color(r, g, b, 0);
        }

        private static Color channelColorMax(int channel, int r, int g, int b, int a) {
            if (channel == CHANNEL_R) return new Color(255, g, b, a);
            if (channel == CHANNEL_G) return new Color(r, 255, b, a);
            if (channel == CHANNEL_B) return new Color(r, g, 255, a);
            return new Color(r, g, b, 255);
        }

        private static Color lerp(Color a, Color b, float t) {
            t = Mth.clamp(t, 0.0f, 1.0f);
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            int al = (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t);
            return new Color(Mth.clamp(r, 0, 255), Mth.clamp(g, 0, 255), Mth.clamp(bl, 0, 255), Mth.clamp(al, 0, 255));
        }
    }

}
