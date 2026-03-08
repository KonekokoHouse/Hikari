package com.github.lumin.modules;

import com.github.lumin.settings.Setting;
import com.github.lumin.settings.impl.*;
import com.github.lumin.utils.player.ChatUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.common.NeoForge;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Module {

    private final String chineseName;

    private final String chineseDescription;

    public Category category;

    private int keyBind = -1;

    public enum BindMode {Toggle, Hold}

    private BindMode bindMode = BindMode.Toggle;

    private boolean enabled;

    public final List<Setting<?>> settings = new ArrayList<>();

    protected static final Minecraft mc = Minecraft.getInstance();

    public Module(String chineseName, String chineseDescription, Category category) {
        this.chineseName = chineseName;
        this.chineseDescription = chineseDescription;
        this.category = category;
    }

    protected boolean nullCheck() {
        return mc.player == null || mc.level == null;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void toggle() {
        enabled = !enabled;

        if (enabled) {
            try {
                NeoForge.EVENT_BUS.register(this);
            } catch (Exception ignored) {
            }

            onEnable();

            ChatUtils.addChatMessage(chineseName + " 已启用");
        } else {
            try {
                NeoForge.EVENT_BUS.unregister(this);
            } catch (Exception ignored) {
            }

            onDisable();

            ChatUtils.addChatMessage(chineseName + " 已禁用");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void reset() {
        setEnabled(false);
        bindMode = BindMode.Toggle;
        for (Setting<?> setting : settings) {
            setting.reset();
        }
    }

    private <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(int keyBind) {
        this.keyBind = keyBind;
    }

    public BindMode getBindMode() {
        return bindMode;
    }

    public void setBindMode(BindMode bindMode) {
        this.bindMode = bindMode;
    }

    public String getName() {
        return chineseName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getDescription() {
        return chineseDescription;
    }

    public String getChineseDescription() {
        return chineseDescription;
    }

    protected IntSetting intSetting(String chineseName, int defaultValue, int min, int max, int step, Setting.Dependency dependency) {
        return addSetting(new IntSetting(chineseName, defaultValue, min, max, step, dependency, false));
    }

    protected IntSetting intSetting(String chineseName, int defaultValue, int min, int max, int step, Setting.Dependency dependency, boolean percentageMode) {
        return addSetting(new IntSetting(chineseName, defaultValue, min, max, step, dependency, percentageMode));
    }

    protected IntSetting intSetting(String chineseName, int defaultValue, int min, int max, int step) {
        return addSetting(new IntSetting(chineseName, defaultValue, min, max, step));
    }

    protected BoolSetting boolSetting(String chineseName, boolean defaultValue, Setting.Dependency dependency) {
        return addSetting(new BoolSetting(chineseName, defaultValue, dependency));
    }

    protected BoolSetting boolSetting(String chineseName, boolean defaultValue) {
        return addSetting(new BoolSetting(chineseName, defaultValue));
    }

    protected DoubleSetting doubleSetting(String chineseName, double defaultValue, double min, double max, double step, Setting.Dependency dependency) {
        return addSetting(new DoubleSetting(chineseName, defaultValue, min, max, step, dependency, false));
    }

    protected DoubleSetting doubleSetting(String chineseName, double defaultValue, double min, double max, double step, Setting.Dependency dependency, boolean percentageMode) {
        return addSetting(new DoubleSetting(chineseName, defaultValue, min, max, step, dependency, percentageMode));
    }

    protected DoubleSetting doubleSetting(String chineseName, double defaultValue, double min, double max, double step) {
        return addSetting(new DoubleSetting(chineseName, defaultValue, min, max, step));
    }

    protected StringSetting stringSetting(String chineseName, String defaultValue, Setting.Dependency dependency) {
        return addSetting(new StringSetting(chineseName, defaultValue, dependency));
    }

    protected StringSetting stringSetting(String chineseName, String defaultValue) {
        return addSetting(new StringSetting(chineseName, defaultValue));
    }

    protected ModeSetting modeSetting(String chineseName, String defaultValue, String[] modes, Setting.Dependency dependency) {
        return addSetting(new ModeSetting(chineseName, defaultValue, modes, dependency));
    }

    protected ModeSetting modeSetting(String chineseName, String defaultValue, String[] modes) {
        return addSetting(new ModeSetting(chineseName, defaultValue, modes));
    }

    protected ColorSetting colorSetting(String chineseName, Color defaultValue, Setting.Dependency dependency) {
        return addSetting(new ColorSetting(chineseName, defaultValue, dependency));
    }

    protected ColorSetting colorSetting(String chineseName, Color defaultValue) {
        return addSetting(new ColorSetting(chineseName, defaultValue));
    }

}