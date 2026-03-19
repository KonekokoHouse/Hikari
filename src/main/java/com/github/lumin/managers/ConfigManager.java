package com.github.lumin.managers;

import com.github.lumin.Lumin;
import com.github.lumin.modules.Module;
import com.github.lumin.settings.Setting;
import com.github.lumin.settings.impl.*;
import com.google.gson.*;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ConfigManager {

    public static final ConfigManager INSTANCE = new ConfigManager();

    private static final int CONFIG_VERSION = 1;

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final Path configFile = Paths.get("hikari-config").resolve("config.json");

    private boolean dirty;
    private boolean modulesApplied;

    private JsonObject root = new JsonObject();

    private ConfigManager() {
    }

    public void initConfig() {
        try {
            loadFromDisk();
            if (!root.has("version")) {
                root.addProperty("version", CONFIG_VERSION);
                dirty = true;
            }
            applyToModules(ModuleManager.INSTANCE.getModules());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized Path getConfigFile() {
        return configFile;
    }

    public synchronized void reload() {
        loadFromDisk();
        modulesApplied = false;
        applyToModules(ModuleManager.INSTANCE.getModules());
    }

    public synchronized void applyToModules(List<Module> modules) {
        if (modules == null) {
            return;
        }

        JsonObject modulesObj = getObject(root, "modules");
        if (modulesObj == null) {
            modulesApplied = true;
            return;
        }

        for (Module module : modules) {
            if (module == null) {
                continue;
            }

            JsonObject moduleObj = getObject(modulesObj, module.getName());
            if (moduleObj == null) {
                continue;
            }

            if (moduleObj.has("keyBind") && moduleObj.get("keyBind").isJsonPrimitive()) {
                try {
                    module.setKeyBind(moduleObj.get("keyBind").getAsInt());
                } catch (Exception ignored) {
                }
            }

            if (moduleObj.has("bindMode") && moduleObj.get("bindMode").isJsonPrimitive()) {
                String mode = moduleObj.get("bindMode").getAsString();
                try {
                    module.setBindMode(Module.BindMode.valueOf(mode));
                } catch (Exception ignored) {
                }
            }

            JsonObject settingsObj = getObject(moduleObj, "settings");
            if (settingsObj != null) {
                for (Setting<?> setting : module.getSettings()) {
                    JsonElement value = settingsObj.get(setting.getName());
                    applySetting(setting, value);
                }
            }

            if (moduleObj.has("enabled") && moduleObj.get("enabled").isJsonPrimitive()) {
                module.setEnabled(moduleObj.get("enabled").getAsBoolean());
            }

        }

        modulesApplied = true;
    }

    public synchronized void saveNow() {
        saveModulesToRoot(ModuleManager.INSTANCE.getModules());
        writeToDiskIfDirty();
    }

    private synchronized void loadFromDisk() {
        try {
            Files.createDirectories(configFile.getParent());
        } catch (IOException ignored) {
        }

        if (!Files.exists(configFile)) {
            root = new JsonObject();
            return;
        }

        try {
            String json = Files.readString(configFile, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed != null && parsed.isJsonObject()) {
                root = parsed.getAsJsonObject();
            } else {
                root = new JsonObject();
            }
        } catch (Exception e) {
            Lumin.LOGGER.error("读取配置文件失败: {}", configFile, e);
            root = new JsonObject();
        }
    }

    private synchronized void saveModulesToRoot(List<Module> modules) {
        if (modules == null) {
            return;
        }

        JsonObject newModulesObj = buildModulesObject(modules);
        JsonElement old = root.get("modules");
        if (old == null || !old.equals(newModulesObj)) {
            root.add("modules", newModulesObj);
            dirty = true;
        }

        if (!root.has("version")) {
            root.addProperty("version", CONFIG_VERSION);
            dirty = true;
        }
    }

    private JsonObject buildModulesObject(List<Module> modules) {
        JsonObject modulesObj = new JsonObject();

        for (Module module : modules) {
            if (module == null) {
                continue;
            }

            JsonObject moduleObj = new JsonObject();
            moduleObj.addProperty("enabled", module.isEnabled());
            moduleObj.addProperty("keyBind", module.getKeyBind());
            moduleObj.addProperty("bindMode", module.getBindMode().name());

            JsonObject settingsObj = new JsonObject();
            for (Setting<?> setting : module.getSettings()) {
                if (setting == null) {
                    continue;
                }
                JsonElement value = serializeSetting(setting);
                if (value != null) {
                    settingsObj.add(setting.getName(), value);
                }
            }
            moduleObj.add("settings", settingsObj);

            modulesObj.add(module.getName(), moduleObj);
        }

        return modulesObj;
    }

    private static JsonObject getObject(JsonObject parent, String key) {
        JsonElement el = parent.get(key);
        if (el == null || !el.isJsonObject()) {
            return null;
        }
        return el.getAsJsonObject();
    }

    private void writeToDiskIfDirty() {
        if (!dirty) {
            return;
        }

        try {
            Files.createDirectories(configFile.getParent());
            String json = gson.toJson(root);
            Files.writeString(
                    configFile,
                    json,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            dirty = false;
        } catch (IOException e) {
            Lumin.LOGGER.error("写入配置文件失败: {}", configFile, e);
        }
    }

    private static JsonElement serializeSetting(Setting<?> setting) {
        if (setting instanceof BoolSetting s) {
            return new JsonPrimitive(s.getValue());
        }
        if (setting instanceof IntSetting s) {
            return new JsonPrimitive(s.getValue());
        }
        if (setting instanceof DoubleSetting s) {
            return new JsonPrimitive(s.getValue());
        }
        if (setting instanceof StringSetting s) {
            return new JsonPrimitive(s.getValue());
        }
        if (setting instanceof EnumSetting s) {
            return new JsonPrimitive(s.getValue().toString());
        }
        if (setting instanceof ColorSetting s) {
            Color c = s.getValue();
            if (c == null) {
                return null;
            }
            return new JsonPrimitive(c.getRGB());
        }
        return null;
    }

    private static void applySetting(Setting<?> setting, JsonElement value) {
        if (value == null || !value.isJsonPrimitive()) {
            return;
        }

        try {
            if (setting instanceof BoolSetting s) {
                s.setValue(value.getAsBoolean());
            } else if (setting instanceof IntSetting s) {
                s.setValue(value.getAsInt());
            } else if (setting instanceof DoubleSetting s) {
                s.setValue(value.getAsDouble());
            } else if (setting instanceof StringSetting s) {
                s.setValue(value.getAsString());
            } else if (setting instanceof EnumSetting s) {
                s.setMode(value.getAsString());
            } else if (setting instanceof ColorSetting s) {
                int argb = value.getAsInt();
                Color c = new Color(argb, true);
                if (!s.isAllowAlpha()) {
                    c = new Color(c.getRed(), c.getGreen(), c.getBlue());
                }
                s.setValue(c);
            }
        } catch (Exception ignored) {
        }
    }

}
