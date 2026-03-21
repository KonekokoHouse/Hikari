package com.github.lumin.gui.dropdown;

import net.minecraft.util.Mth;

import java.awt.*;

public final class DropdownTheme {

    public static final Color SCRIM = new Color(0, 0, 0, 152);
    public static final Color SHADOW = new Color(0, 0, 0, 82);

    public static final Color SURFACE = new Color(20, 18, 24, 238);
    public static final Color SURFACE_DIM = new Color(15, 13, 19, 232);
    public static final Color SURFACE_CONTAINER_LOW = new Color(29, 27, 32, 240);
    public static final Color SURFACE_CONTAINER = new Color(33, 31, 38, 244);
    public static final Color SURFACE_CONTAINER_HIGH = new Color(43, 41, 48, 248);
    public static final Color SURFACE_CONTAINER_HIGHEST = new Color(54, 52, 59, 252);

    public static final Color OUTLINE = new Color(147, 143, 153, 180);
    public static final Color OUTLINE_SOFT = new Color(147, 143, 153, 96);

    public static final Color PRIMARY = new Color(208, 188, 255);
    public static final Color ON_PRIMARY = new Color(56, 30, 114);
    public static final Color PRIMARY_CONTAINER = new Color(79, 55, 139, 236);
    public static final Color ON_PRIMARY_CONTAINER = new Color(234, 221, 255);

    public static final Color SECONDARY = new Color(204, 194, 220);
    public static final Color ON_SECONDARY = new Color(51, 45, 65);
    public static final Color SECONDARY_CONTAINER = new Color(74, 68, 88, 236);
    public static final Color ON_SECONDARY_CONTAINER = new Color(232, 222, 248);

    public static final Color TERTIARY = new Color(239, 184, 200);
    public static final Color ON_TERTIARY = new Color(73, 37, 50);
    public static final Color TERTIARY_CONTAINER = new Color(99, 59, 72, 236);
    public static final Color ON_TERTIARY_CONTAINER = new Color(255, 216, 228);
    public static final Color INVERSE_SURFACE = new Color(230, 224, 233);
    public static final Color INVERSE_ON_SURFACE = new Color(49, 48, 51);

    public static final Color TEXT_PRIMARY = new Color(230, 224, 233);
    public static final Color TEXT_SECONDARY = new Color(202, 196, 208);
    public static final Color TEXT_MUTED = new Color(147, 143, 153);
    public static final Color SUCCESS = new Color(204, 194, 220);
    public static final Color ERROR = new Color(242, 184, 181);

    public static final int PANEL_RADIUS = 16;
    public static final int SECTION_RADIUS = 12;
    public static final int CARD_RADIUS = 9;
    public static final int CHIP_RADIUS = 999;

    public static final float OUTER_PADDING = 9.0f;
    public static final float SECTION_GAP = 7.0f;
    public static final float INNER_PADDING = 8.0f;
    public static final float ROW_GAP = 4.0f;
    public static final float PANEL_TITLE_INSET = 10.0f;
    public static final float PANEL_VIEWPORT_INSET = 7.0f;
    public static final float ROW_CONTENT_INSET = 8.0f;
    public static final float ROW_TRAILING_INSET = 8.0f;
    public static final float RAIL_COLLAPSED_WIDTH = 52.0f;
    public static final float RAIL_EXPANDED_WIDTH = 182.0f;

    private DropdownTheme() {
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color lerp(Color start, Color end, float delta) {
        float t = Mth.clamp(delta, 0.0f, 1.0f);
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

}
