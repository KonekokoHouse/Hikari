package com.github.lumin.assets.i18n;

import com.github.lumin.managers.impl.ModuleManager;
import com.github.lumin.modules.Category;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;
import com.github.lumin.settings.impl.EnumSetting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class I18NFileGenerator {

    public static void generate(String filePath) {
        JsonObject root = new JsonObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (Category category : Category.values()) {
            String catKey = "lumin.categories." + category.toString().toLowerCase();
            root.addProperty(catKey, "");
        }

        for (Module module : ModuleManager.INSTANCE.getModules()) {
            String modulePrefix = "modules." + module.getName().toLowerCase();
            root.addProperty("lumin." + modulePrefix, "");

            for (Setting<?> setting : module.getSettings()) {
                String settingKey = "lumin." + modulePrefix + "." + setting.getName().toLowerCase();
                root.addProperty(settingKey, "");

                if (setting instanceof EnumSetting<?> enumSetting) {
                    for (final var mode : enumSetting.getModes()) {
                        root.addProperty(settingKey + "." + mode.toString().toLowerCase(), "");
                    }
                }
            }
        }

        final var file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(root, writer);
            System.out.println("I18N file generated successfully at: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
