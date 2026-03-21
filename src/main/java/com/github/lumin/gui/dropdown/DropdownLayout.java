package com.github.lumin.gui.dropdown;

public final class DropdownLayout {

    private DropdownLayout() {
    }

    public static Layout compute(int screenWidth, int screenHeight) {
        float panelWidth = Math.min(760.0f, screenWidth - 68.0f);
        float panelHeight = Math.min(438.0f, screenHeight - 72.0f);

        float x = (screenWidth - panelWidth) / 2.0f;
        float y = (screenHeight - panelHeight) / 2.0f;

        float railWidth = 64.0f;
        float gap = DropdownTheme.SECTION_GAP;
        float moduleWidth = Math.min(224.0f, panelWidth * 0.32f);
        float detailWidth = panelWidth - railWidth - moduleWidth - gap * 2.0f;

        Rect panel = new Rect(x, y, panelWidth, panelHeight);
        Rect rail = new Rect(x + DropdownTheme.OUTER_PADDING, y + DropdownTheme.OUTER_PADDING, railWidth, panelHeight - DropdownTheme.OUTER_PADDING * 2.0f);
        Rect modules = new Rect(rail.right() + gap, y + DropdownTheme.OUTER_PADDING, moduleWidth, panelHeight - DropdownTheme.OUTER_PADDING * 2.0f);
        Rect detail = new Rect(modules.right() + gap, y + DropdownTheme.OUTER_PADDING, detailWidth - DropdownTheme.OUTER_PADDING * 2.0f, panelHeight - DropdownTheme.OUTER_PADDING * 2.0f);

        return new Layout(panel, rail, modules, detail);
    }

    public record Layout(Rect panel, Rect rail, Rect modules, Rect detail) {
    }

    public record Rect(float x, float y, float width, float height) {
        public float right() {
            return x + width;
        }

        public float bottom() {
            return y + height;
        }

        public float centerX() {
            return x + width / 2.0f;
        }

        public float centerY() {
            return y + height / 2.0f;
        }

        public boolean contains(double px, double py) {
            return px >= x && px <= right() && py >= y && py <= bottom();
        }

        public Rect inset(float amount) {
            return new Rect(x + amount, y + amount, width - amount * 2.0f, height - amount * 2.0f);
        }
    }
}
