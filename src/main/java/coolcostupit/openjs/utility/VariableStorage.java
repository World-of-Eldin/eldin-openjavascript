/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

// TODO: Remove in 1.3.0 (Deprecated due to memory leak)
@SuppressWarnings("all")
public class VariableStorage {
    private final JavaPlugin plugin;
    private final File storageFile;
    private final Map<String, Object> globalVars;
    private final Map<String, Map<String, Object>> scriptVars;

    public VariableStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "variableStorage.dat");
        this.globalVars = new HashMap<>();
        this.scriptVars = new HashMap<>();
        loadVariables();
    }

    public Object getStoredVar(String scriptName, String varName, Object defaultVar, boolean global) {
        sharedClass.logger.log(Level.WARNING, "["+scriptName+"] Do not use loadVar(...) use DiskApi instead!", pluginLogger.RED);
        if (global) {
            return globalVars.computeIfAbsent(varName, k -> defaultVar);
        } else {
            Map<String, Object> scriptSpecificVars = scriptVars.computeIfAbsent(scriptName, k -> new HashMap<>());
            return scriptSpecificVars.computeIfAbsent(varName, k -> defaultVar);
        }
    }

    public void setStoredVar(String scriptName, String varName, Object var, boolean global) {
        sharedClass.logger.log(Level.WARNING, "["+scriptName+"] Do not use getVar(...) use DiskApi instead!", pluginLogger.RED);
        if (global) {
            globalVars.put(varName, var);
        } else {
            Map<String, Object> scriptSpecificVars = scriptVars.computeIfAbsent(scriptName, k -> new HashMap<>());
            scriptSpecificVars.put(varName, var);
        }
    }

    public void saveVariables() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) {
            oos.writeObject(globalVars);
            oos.writeObject(scriptVars);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save variables: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadVariables() {
        if (!storageFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile))) {
            Map<String, Object> loadedGlobalVars = (Map<String, Object>) ois.readObject();
            Map<String, Map<String, Object>> loadedScriptVars = (Map<String, Map<String, Object>>) ois.readObject();

            globalVars.putAll(loadedGlobalVars);
            scriptVars.putAll(loadedScriptVars);
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to load variables: " + e.getMessage());
        }
    }
}
