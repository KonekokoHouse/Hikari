package com.github.lumin.assets.i18n;

import com.github.lumin.managers.impl.TranslateManager;
import net.minecraft.client.resources.language.I18n;

public class TranslateComponent {

    public final String prefix;
    public final String suffix;
    public final String fullKey;
    public final String fullKeyWithoutLumin;

    private TranslateComponent(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.fullKey = LUMIN_PREFIX + "." + prefix + "." + suffix;
        this.fullKeyWithoutLumin = prefix + "." + suffix;
    }

    private String cachedName;

    public static String LUMIN_PREFIX = "lumin";

    public static TranslateComponent create(String prefix, String suffix) {
        TranslateComponent component = new TranslateComponent(prefix, suffix);
        TranslateManager.INSTANCE.registerTranslateComponent(component);
        return component;
    }

    public String getTranslatedName() {
        if (cachedName == null) {
            cachedName = I18n.get(fullKey);
        }
        return cachedName;
    }

    public void refresh() {
        cachedName = I18n.get(fullKey);
    }

}