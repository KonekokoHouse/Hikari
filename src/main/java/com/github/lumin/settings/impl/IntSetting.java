package com.github.lumin.settings.impl;

import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;
import net.minecraft.util.Mth;

public class IntSetting extends Setting<Integer> {

    private final int min;
    private final int max;
    private final int step;
    private final boolean percentageMode;

    public IntSetting(String name, Module module, int defaultValue, int min, int max, int step) {
        this(name, module, defaultValue, min, max, step, () -> true, false);
    }

    public IntSetting(String name, Module module, int defaultValue, int min, int max, int step, boolean percentageMode) {
        this(name, module, defaultValue, min, max, step, () -> true, percentageMode);
    }

    public IntSetting(String name, Module module, int defaultValue, int min, int max, int step, Dependency dependency, boolean percentageMode) {
        super(name, module, dependency);
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
        this.percentageMode = percentageMode;
    }

    @Override
    public void setValue(Integer value) {
        super.setValue(Mth.clamp(value, min, max));
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getStep() {
        return step;
    }

    public boolean isPercentageMode() {
        return percentageMode;
    }
}