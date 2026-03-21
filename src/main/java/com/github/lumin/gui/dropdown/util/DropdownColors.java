package com.github.lumin.gui.dropdown.util;

import net.minecraft.util.Mth;

import java.awt.*;

public final class DropdownColors {

    private DropdownColors() {
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color mix(Color start, Color end, float delta) {
        float t = Mth.clamp(delta, 0.0f, 1.0f);
        int red = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int green = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int blue = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int alpha = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(red, green, blue, alpha);
    }
}
