/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.pluginbridges;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class PlaceHolderApiJS {
    private static final Map<String, PlaceholderData> registeredPlaceholders = new HashMap<>();

    public static class PlaceholderData {
        public final Object handler;
        public final ScriptEngine engine;
        public final String scriptName;

        public PlaceholderData(Object handler, ScriptEngine engine, String scriptName) {
            this.handler = handler;
            this.engine = engine;
            this.scriptName = scriptName;
        }
    }

    public PlaceHolderApiJS() {
    }

    public String parseString(Player player, @NotNull String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    public void registerPlaceholder(String prefix, Object handler, String scriptName, ScriptEngine engine) {
        if (registeredPlaceholders.containsKey(prefix)) {
            sharedClass.logger.log(Level.WARNING, "[" + scriptName + "] Placeholder %" + sharedClass.Identifier + "_" + prefix + "% already exists and will be overwritten.", pluginLogger.ORANGE);
        }
        if (prefix.contains("_")) {
            sharedClass.logger.log(Level.WARNING, "[" + scriptName + "] Placeholder '%" + sharedClass.Identifier + "_" + prefix + "%' contains an underscore, which will make it unusable due to parameter splitting!", pluginLogger.ORANGE);
        }
        registeredPlaceholders.put(prefix, new PlaceholderData(handler, engine, scriptName));
        if (sharedClass.configUtil.getConfigFromBuffer("LogPlaceHolderActivity", true)) {
            sharedClass.logger.log(Level.INFO, "[" + scriptName + "] Placeholder %" + sharedClass.Identifier + "_" + prefix + "% has been registered.", pluginLogger.GREEN);
        }
    }

    public boolean unregisterPlaceholder(String prefix, String scriptName) {
        PlaceholderData data = registeredPlaceholders.get(prefix);
        if (data != null && data.scriptName.equals(scriptName)) {
            registeredPlaceholders.remove(prefix);
            if (sharedClass.configUtil.getConfigFromBuffer("LogPlaceHolderActivity", true)) {
                sharedClass.logger.log(Level.INFO, "[" + scriptName + "] Placeholder %" + sharedClass.Identifier + "_" + prefix + "% has been unregistered.", pluginLogger.LIGHT_BLUE);
            }
            return true;
        }
        return false;
    }

    public void unregisterPlaceholders(String scriptName) {
        registeredPlaceholders.entrySet().removeIf(entry -> {
            boolean match = entry.getValue().scriptName.equals(scriptName);
            if (match) {
                if (sharedClass.configUtil.getConfigFromBuffer("LogPlaceHolderActivity", true)) {
                    sharedClass.logger.log(Level.INFO, "[" + scriptName + "] Placeholder %" + sharedClass.Identifier + "_" + entry.getKey() + "% has been unregistered.", pluginLogger.LIGHT_BLUE);
                }
            }
            return match;
        });
    }

    public String invokePrefix(String prefix, Player player, String params) {
        PlaceholderData data = registeredPlaceholders.get(prefix);
        if (data == null) return null;
        try {
            return (String) ((Invocable) data.engine).invokeMethod(data.handler, "onRequest", player, params);
        } catch (Exception e) {
            sharedClass.logger.log(Level.SEVERE, "[" + data.scriptName + "] Error invoking placeholder %" + sharedClass.Identifier + "_" + prefix + "% reason: " + e.getMessage(), pluginLogger.RED);
            return null;
        }
    }
}
