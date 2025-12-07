/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class configurationUtil {
    private final JavaPlugin plugin;
    public final Map<String, Object> configBuffer = new HashMap<>();

    public configurationUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    private void saveConfig() {
        plugin.saveConfig();
    }

    public void loadBufferFromConfig() {
        FileConfiguration config = getConfig();
        for (String key : config.getKeys(false)) {
            configBuffer.put(key, config.get(key));
        }
    }

    public void saveBufferToConfig() {
        FileConfiguration config = getConfig();
        for (Map.Entry<String, Object> entry : configBuffer.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        saveConfig();
    }

    @SuppressWarnings("unchecked")
    public <T> T getConfigFromBuffer(String configName, T defaultValue) {
        if (configBuffer.containsKey(configName)) {
            Object value = configBuffer.get(configName);
            if (defaultValue instanceof Boolean) {
                return (T) Boolean.valueOf(value.toString());
            } else if (defaultValue instanceof Integer) {
                return (T) Integer.valueOf(value.toString());
            } else if (defaultValue instanceof Double) {
                return (T) Double.valueOf(value.toString());
            } else if (defaultValue instanceof String) {
                return (T) value.toString();
            } else {
                return (T) value;
            }
        }
        setConfigInBuffer(configName, defaultValue);
        return defaultValue;
    }

    public void setConfigInBuffer(String configName, Object value) {
        configBuffer.put(configName, value);
    }

    public void reloadConfigBuffer() {
        plugin.reloadConfig();
        configBuffer.clear();
        loadBufferFromConfig();
    }
}
