package com.github.lumin.gui.clickgui.component;

import com.github.lumin.assets.i18n.TranslateComponent;
import com.github.lumin.gui.Component;
import com.github.lumin.gui.IComponent;
import com.github.lumin.gui.clickgui.component.impl.*;
import com.github.lumin.modules.Module;
import com.github.lumin.modules.impl.client.ClickGui;
import com.github.lumin.settings.Setting;
import com.github.lumin.settings.impl.*;
import com.github.lumin.utils.render.MouseUtils;
import com.github.lumin.utils.render.animation.Animation;
import com.github.lumin.utils.render.animation.Easing;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ModuleComponent implements IComponent {

    private final Module module;
    private float x, y, width, height;
    private final List<Component> settings = new CopyOnWriteArrayList<>();
    private String filterTextLower = "";
    private float lastBindBoxX, lastBindBoxY, lastBindBoxW, lastBindBoxH;
    private float lastBindModeX, lastBindModeY, lastBindModeW, lastBindModeH;
    private boolean bindingKey;
    private float lastBindListenX, lastBindListenY, lastBindListenW, lastBindListenH;

    private final Animation bgAnimation = new Animation(Easing.EASE_OUT_EXPO, 450L);
    private final Animation exitAnimation = new Animation(Easing.EASE_OUT_EXPO, 450L);
    private float sourceX, sourceY, sourceW, sourceH;
    private float targetCardX, targetCardY, targetCardW, targetCardH;
    private float exitStartX, exitStartY, exitStartW, exitStartH;
    private boolean animationInitialized = false;
    private boolean isExiting = false;
    private boolean exitAnimationPrepared = false;

    private final TranslateComponent keyBindNoneComponent = TranslateComponent.create("keybind", "none");
    private final TranslateComponent keyBindToggleComponent = TranslateComponent.create("keybind", "toggle");
    private final TranslateComponent keyBindHoldComponent = TranslateComponent.create("keybind", "hold");

    public void initAnimation(float sourceX, float sourceY, float sourceW, float sourceH, float targetX, float targetY, float targetW, float targetH) {
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceW = sourceW;
        this.sourceH = sourceH;
        this.x = targetX;
        this.y = targetY;
        this.width = targetW;
        this.height = targetH;
        this.bgAnimation.setStartValue(0.0f);
        this.bgAnimation.run(1.0f);
        this.exitAnimation.setStartValue(1.0f);
        this.exitAnimation.run(1.0f);
        this.animationInitialized = true;
        this.isExiting = false;
    }

    public void startExitAnimation(float cardX, float cardY, float cardW, float cardH) {
        this.exitStartX = x;
        this.exitStartY = y;
        this.exitStartW = width;
        this.exitStartH = height;
        this.targetCardX = cardX;
        this.targetCardY = cardY;
        this.targetCardW = cardW;
        this.targetCardH = cardH;
        this.exitAnimation.setStartValue(1.0f);
        this.exitAnimation.run(0.0f);
        this.isExiting = true;
        this.exitAnimationPrepared = true;
    }

    public boolean isAnimationFinished() {
        return isExiting && exitAnimation.getValue() <= 0.01f;
    }

    public boolean isExiting() {
        return isExiting;
    }

    public ModuleComponent(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            Component component = createSettingComponent(setting);
            if (component != null) {
                settings.add(component);
            }
        }
    }

    private Component createSettingComponent(Setting<?> setting) {
        if (setting instanceof BoolSetting boolValue) return new BoolSettingComponent(boolValue);
        if (setting instanceof IntSetting intSetting) return new NumberSettingComponent(intSetting);
        if (setting instanceof DoubleSetting doubleSetting) return new NumberSettingComponent(doubleSetting);
        if (setting instanceof EnumSetting enumSetting) return new EnumSettingComponent(enumSetting);
        if (setting instanceof ColorSetting colorSetting) return new ColorSettingComponent(colorSetting);
        if (setting instanceof StringSetting stringSetting) return new StringSettingComponent(stringSetting);
        return null;
    }

    @Override
    public void render(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        float guiScale = ClickGui.INSTANCE.scale.getValue().floatValue();
        float padding = 8.0f * guiScale;
        float rowH = 18.0f * guiScale;
        float rowGap = 4.0f * guiScale;
        float radius = 8.0f * guiScale;

        if (!isExiting) {
            bgAnimation.run(1.0f);
        } else {
            exitAnimation.run(0.0f);
        }
        float progress = Mth.clamp(isExiting ? exitAnimation.getValue() : bgAnimation.getValue(), 0.0f, 1.0f);

        float animX = x;
        float animY = y;
        float animW = width;
        float animH = height;
        float animRadius = radius;

        if (animationInitialized) {
            if (isExiting && exitAnimationPrepared) {
                animX = targetCardX + (exitStartX - targetCardX) * progress;
                animY = targetCardY + (exitStartY - targetCardY) * progress;
                animW = targetCardW + (exitStartW - targetCardW) * progress;
                animH = targetCardH + (exitStartH - targetCardH) * progress;
            } else {
                animX = sourceX + (x - sourceX) * progress;
                animY = sourceY + (y - sourceY) * progress;
                animW = sourceW + (width - sourceW) * progress;
                animH = sourceH + (height - sourceH) * progress;
            }
            animRadius = radius * progress + (sourceW * 0.1f) * (1.0f - progress);
        }

        if (animW > 0.5f && animH > 0.5f) {
            set.bottomRoundRect().addRoundRect(animX, animY, animW, animH, animRadius, new Color(25, 25, 25, 140));
        }

        if (progress < 0.99f) {
            float oldContentAlpha = 1.0f - progress;
            int oldAlphaInt = (int) (255 * oldContentAlpha);

            float centerX = animX + animW / 2.0f;
            float centerY = animY + animH / 2.0f;

            float oldNameScale = 1.1f * guiScale;
            float oldDescScale = 0.62f * guiScale;

            float oldNameW = set.font().getWidth(module.getTranslatedName(), oldNameScale);
            float oldDescW = set.font().getWidth(module.getDescription(), oldDescScale);

            float oldNameH = set.font().getHeight(oldNameScale);
            float oldDescH = set.font().getHeight(oldDescScale);
            float oldBlockH = oldNameH + 3 * guiScale + oldDescH;

            float oldStartY = centerY - oldBlockH / 2.0f;

            set.font().addText(module.getTranslatedName(), centerX - oldNameW / 2.0f, oldStartY - 0.6f * guiScale, oldNameScale, new Color(255, 255, 255, oldAlphaInt));
            set.font().addText(module.getDescription(), centerX - oldDescW / 2.0f, oldStartY + oldNameH + 3 * guiScale - 0.2f * guiScale, oldDescScale, new Color(200, 200, 200, oldAlphaInt));
        }

        if (progress <= 0.01f) return;
        float detailProgress = Mth.clamp((progress - 0.65f) / 0.35f, 0.0f, 1.0f);
        if (detailProgress <= 0.01f) return;

        int contentAlpha = (int) (255 * detailProgress);
        Color textColor = new Color(255, 255, 255, contentAlpha);
        Color dimTextColor = new Color(200, 200, 200, contentAlpha);
        Color boxBgColor = new Color(0, 0, 0, (int) (70 * detailProgress));
        Color selectedBgColor = new Color(255, 255, 255, (int) (26 * detailProgress));
        Color dividerColor = new Color(255, 255, 255, (int) (14 * detailProgress));

        float titleScale = 1.15f * guiScale * (0.92f + 0.08f * detailProgress);
        float titleY = animY + padding - guiScale;
        set.font().addText(module.getTranslatedName(), animX + padding, titleY, titleScale, textColor);

        float titleH = set.font().getHeight(titleScale);
        float headerY = titleY - guiScale;
        float headerH = titleH + 4.0f * guiScale;

        String bindText = getKeyBindText();
        float bindTextScale = 0.85f * guiScale;
        float bindTextW = set.font().getWidth(bindText, bindTextScale);

        float bindPad = 6.0f * guiScale;
        float bindBoxW = Math.max(40.0f * guiScale, bindTextW + bindPad * 2.0f);

        String mode0 = keyBindToggleComponent.getTranslatedName();
        String mode1 = keyBindHoldComponent.getTranslatedName();
        float modeTextScale = 0.80f * guiScale;
        float modePad = 7.0f * guiScale;
        float segW = Math.max(set.font().getWidth(mode0, modeTextScale), set.font().getWidth(mode1, modeTextScale)) + modePad * 2.0f;
        float modeW = segW * 2.0f;

        float gap = 6.0f * guiScale;
        float totalW = bindBoxW + gap + modeW;
        float rightX = animX + animW - padding - totalW;

        lastBindBoxX = rightX;
        lastBindBoxY = headerY;
        lastBindBoxW = bindBoxW;
        lastBindBoxH = headerH;

        float bindRadius = Math.min(6.0f * guiScale, headerH / 2.0f);
        set.bottomRoundRect().addRoundRect(rightX, headerY, bindBoxW, headerH, bindRadius, boxBgColor);

        float bindTextX = rightX + (bindBoxW - bindTextW) / 2.0f;
        float bindTextY = headerY + (headerH - set.font().getHeight(bindTextScale)) / 2.0f - guiScale;
        if (!bindingKey) {
            set.font().addText(bindText, bindTextX, bindTextY, bindTextScale, dimTextColor);
        }

        lastBindListenX = rightX;
        lastBindListenY = headerY;
        lastBindListenW = bindBoxW;
        lastBindListenH = headerH;

        if (bindingKey) {
            Color listenBg = new Color(255, 255, 255, (int) (22 * detailProgress));
            set.bottomRoundRect().addRoundRect(lastBindListenX, lastBindListenY, lastBindListenW, lastBindListenH, bindRadius, listenBg);

            if (System.currentTimeMillis() % 1000 > 500) {
                float lineW = 10.0f * guiScale;
                float lineH = 1.5f * guiScale;
                float lineX = lastBindListenX + (lastBindListenW - lineW) / 2.0f;
                float lineY = lastBindListenY + (lastBindListenH / 2.0f) + 3.0f * guiScale;
                set.bottomRoundRect().addRoundRect(lineX, lineY, lineW, lineH, 0.0f, dimTextColor);
            }
        }

        float modeX = rightX + bindBoxW + gap;

        lastBindModeX = modeX;
        lastBindModeY = headerY;
        lastBindModeW = modeW;
        lastBindModeH = headerH;

        float modeRadius = Math.min(6.0f * guiScale, headerH / 2.0f);
        set.bottomRoundRect().addRoundRect(modeX, headerY, modeW, headerH, modeRadius, boxBgColor);

        int selectedIndex = module.getBindMode() == Module.BindMode.Hold ? 1 : 0;
        if (selectedIndex == 0) {
            set.bottomRoundRect().addRoundRect(modeX, headerY, segW, headerH, modeRadius, 0.0f, 0.0f, modeRadius, selectedBgColor);
        } else {
            set.bottomRoundRect().addRoundRect(modeX + segW, headerY, segW, headerH, 0.0f, modeRadius, modeRadius, 0.0f, selectedBgColor);
        }

        set.bottomRoundRect().addRoundRect(modeX + segW, headerY + 2.0f * guiScale, 1.0f * guiScale, headerH - 4.0f * guiScale, 0.0f, dividerColor);

        float modeTextY = headerY + (headerH - set.font().getHeight(modeTextScale)) / 2.0f - guiScale;
        float mode0W = set.font().getWidth(mode0, modeTextScale);
        float mode1W = set.font().getWidth(mode1, modeTextScale);
        set.font().addText(mode0, modeX + (segW - mode0W) / 2.0f, modeTextY, modeTextScale, selectedIndex == 0 ? textColor : dimTextColor);
        set.font().addText(mode1, modeX + segW + (segW - mode1W) / 2.0f, modeTextY, modeTextScale, selectedIndex == 1 ? textColor : dimTextColor);

        float cursorY = animY + padding + set.font().getHeight(titleScale) + 6.0f * guiScale;
        float itemX = animX + padding;
        float itemW = Math.max(0.0f, animW - padding * 2);

        if (detailProgress > 0.01f) {
            float bgBottom = animY + animH;
            int visibleSettingIndex = 0;
            int totalVisible = getFilteredVisibleCount();
            float maxDelay = Math.min(0.3f, Math.max(0.0f, (totalVisible - 1) * 0.02f));
            for (Component setting : settings) {
                if (!isSettingVisible(setting)) continue;
                if (cursorY + rowH > bgBottom) break;
                float rowDelay = 0.02f;
                float rowProgress;
                if (detailProgress >= 1.0f || maxDelay <= 0.0f) {
                    rowProgress = 1.0f;
                } else {
                    rowProgress = Mth.clamp((detailProgress - visibleSettingIndex * rowDelay) / (1.0f - maxDelay), 0.0f, 1.0f);
                }
                if (rowProgress <= 0.0f) {
                    cursorY += rowH + rowGap;
                    visibleSettingIndex++;
                    continue;
                }
                setting.setScale(guiScale);
                setting.setAlpha(rowProgress);
                setting.setX(itemX);
                setting.setY(cursorY + (1.0f - rowProgress) * 6.0f * guiScale);
                setting.setWidth(itemW);
                setting.setHeight(rowH);
                setting.render(set, mouseX, mouseY, partialTicks);
                cursorY += rowH + rowGap;
                visibleSettingIndex++;
            }
        }
    }

    public void renderOverlays(RendererSet set, int mouseX, int mouseY, float partialTicks) {
        forEachVisibleSetting(setting -> {
            if (setting instanceof ColorSettingComponent c && c.isOpened()) {
                c.renderOverlay(set, mouseX, mouseY, partialTicks);
            }
        });
    }

    public void renderOverlayBlurs(int mouseX, int mouseY, float partialTicks) {
        forEachVisibleSetting(setting -> {
            if (setting instanceof ColorSettingComponent c && c.isOpened()) {
                c.renderOverlayBlur(mouseX, mouseY, partialTicks);
            }
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        if (dispatchToOpenedColorPickers(setting -> setting.mouseClicked(event, focused))) return true;

        if (event.button() == 0) {
            if (MouseUtils.isHovering(lastBindModeX, lastBindModeY, lastBindModeW, lastBindModeH, event.x(), event.y())) {
                float segW = lastBindModeW / 2.0f;
                if (segW > 0.0f) {
                    int index = (int) ((event.x() - lastBindModeX) / segW);
                    index = Math.max(0, Math.min(index, 1));
                    module.setBindMode(index == 0 ? Module.BindMode.Toggle : Module.BindMode.Hold);
                }
                return true;
            }
            if (MouseUtils.isHovering(lastBindBoxX, lastBindBoxY, lastBindBoxW, lastBindBoxH, event.x(), event.y())) {
                bindingKey = true;
                return true;
            }
            if (bindingKey && !MouseUtils.isHovering(lastBindListenX, lastBindListenY, lastBindListenW, lastBindListenH, event.x(), event.y())) {
                bindingKey = false;
            }
        }
        if (event.button() == 1 && bindingKey) {
            bindingKey = false;
            return true;
        }
        boolean handled = isHovered((int) event.x(), (int) event.y())
                && dispatchToVisibleSettings(setting -> setting.mouseClicked(event, focused));
        return handled || IComponent.super.mouseClicked(event, focused);
    }

    private String getKeyBindText() {
        int keyBind = module.getKeyBind();
        if (keyBind <= 0) return keyBindNoneComponent.getTranslatedName();
        int scancode = GLFW.glfwGetKeyScancode(keyBind);
        String name = GLFW.glfwGetKeyName(keyBind, scancode);
        if (name != null && !name.isEmpty()) {
            if (name.length() == 1) return name.toUpperCase();
            return name;
        }
        return switch (keyBind) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_SUPER -> "LSUPER";
            case GLFW.GLFW_KEY_RIGHT_SUPER -> "RSUPER";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NUM";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "SCRL";
            case GLFW.GLFW_KEY_PAUSE -> "PAUSE";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "PRTSCR";
            default -> {
                if (keyBind >= GLFW.GLFW_KEY_F1 && keyBind <= GLFW.GLFW_KEY_F25) {
                    yield "F" + (keyBind - GLFW.GLFW_KEY_F1 + 1);
                }
                yield "KEY_" + keyBind;
            }
        };
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dispatchToOpenedColorPickers(setting -> setting.mouseReleased(event))) return true;
        if (hasDraggingSetting() || isHovered((int) event.x(), (int) event.y())) {
            if (dispatchToVisibleSettings(setting -> setting.mouseReleased(event))) return true;
        }
        return IComponent.super.mouseReleased(event);
    }

    public boolean hasDraggingSetting() {
        return anyVisibleSetting(setting -> {
            if (setting instanceof NumberSettingComponent numberSettingComponent)
                return numberSettingComponent.isDragging();
            return false;
        });
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (dispatchToOpenedColorPickers(setting -> setting.keyPressed(event))) return true;
        if (bindingKey) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                bindingKey = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                module.setKeyBind(0);
                bindingKey = false;
                return true;
            }
            if (key != GLFW.GLFW_KEY_UNKNOWN) {
                module.setKeyBind(key);
                bindingKey = false;
                return true;
            }
        }
        boolean handled = dispatchToVisibleSettings(setting -> setting.keyPressed(event));
        return handled || IComponent.super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (dispatchToOpenedColorPickers(setting -> setting.charTyped(input))) return true;
        if (bindingKey) {
            return true;
        }
        boolean handled = dispatchToVisibleSettings(setting -> setting.charTyped(input));
        return handled || IComponent.super.charTyped(input);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Module getModule() {
        return module;
    }

    public List<Component> getSettings() {
        return settings;
    }

    public void setFilterText(String text) {
        if (text == null || text.isEmpty()) {
            filterTextLower = "";
            return;
        }
        filterTextLower = text.toLowerCase();
    }

    public int getFilteredVisibleCount() {
        int count = 0;
        for (Component setting : settings) {
            if (isSettingVisible(setting)) count++;
        }
        return count;
    }

    public void setX(float x) {
        if (isExiting) return;
        this.x = x;
    }

    public void setY(float y) {
        if (isExiting) return;
        this.y = y;
    }

    public void setWidth(float width) {
        if (isExiting) return;
        this.width = width;
    }

    public void setHeight(float height) {
        if (isExiting) return;
        this.height = height;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return MouseUtils.isHovering(x, y, width, height, mouseX, mouseY);
    }

    private boolean isSettingVisible(Component component) {
        if (!component.isVisible()) return false;
        if (!filterTextLower.isEmpty()) {
            String name = getSettingDisplayName(component);
            if (name == null || !name.toLowerCase().startsWith(filterTextLower)) return false;
        }
        return isSettingAvailable(component);
    }

    private boolean isSettingAvailable(Component component) {
        if (component instanceof BoolSettingComponent c) return c.getSetting().isAvailable();
        if (component instanceof NumberSettingComponent c) {
            Object s = c.getSetting();
            if (s instanceof IntSetting is) return is.isAvailable();
            if (s instanceof DoubleSetting ds) return ds.isAvailable();
        }
        if (component instanceof EnumSettingComponent c) return c.getSetting().isAvailable();
        if (component instanceof ColorSettingComponent c) return c.getSetting().isAvailable();
        if (component instanceof StringSettingComponent c) return c.getSetting().isAvailable();
        return true;
    }

    private String getSettingDisplayName(Component component) {
        if (component instanceof BoolSettingComponent c) return c.getSetting().getDisplayName();
        if (component instanceof NumberSettingComponent c) {
            Object s = c.getSetting();
            if (s instanceof IntSetting is) return is.getDisplayName();
            if (s instanceof DoubleSetting ds) return ds.getDisplayName();
        }
        if (component instanceof EnumSettingComponent c) return c.getSetting().getDisplayName();
        if (component instanceof ColorSettingComponent c) return c.getSetting().getDisplayName();
        if (component instanceof StringSettingComponent c) return c.getSetting().getDisplayName();
        return null;
    }

    private boolean dispatchToOpenedColorPickers(Predicate<ColorSettingComponent> handler) {
        for (Component setting : settings) {
            if (!isSettingVisible(setting)) continue;
            if (setting instanceof ColorSettingComponent colorSetting && colorSetting.isOpened()) {
                if (handler.test(colorSetting)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dispatchToVisibleSettings(Predicate<Component> handler) {
        boolean handled = false;
        for (Component setting : settings) {
            if (!isSettingVisible(setting)) continue;
            if (handler.test(setting)) handled = true;
        }
        return handled;
    }

    private boolean anyVisibleSetting(Predicate<Component> predicate) {
        for (Component setting : settings) {
            if (!isSettingVisible(setting)) continue;
            if (predicate.test(setting)) return true;
        }
        return false;
    }

    private void forEachVisibleSetting(java.util.function.Consumer<Component> action) {
        for (Component setting : settings) {
            if (!isSettingVisible(setting)) continue;
            action.accept(setting);
        }
    }

}
