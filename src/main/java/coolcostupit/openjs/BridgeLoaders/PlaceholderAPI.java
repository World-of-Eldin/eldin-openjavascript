/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.BridgeLoaders;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.scriptWrapper;
import coolcostupit.openjs.modules.sharedClass;
import coolcostupit.openjs.pluginbridges.PlaceHolderApiJS;

import javax.script.ScriptEngine;
import java.util.logging.Level;

public class PlaceholderAPI {
    public static PlaceHolderApiJS placeholderApiJS;

    public void Load(String ScriptName, ScriptEngine Engine) {
        try {
            // Lazy loader for PlaceholderApi support
            if (placeholderApiJS == null) {
                if (sharedClass.IsPapiLoaded) {
                    placeholderApiJS = new PlaceHolderApiJS();
                    sharedClass.logger.scriptlog(Level.INFO, ScriptName, "PlaceholderApi support loaded!", pluginLogger.GREEN);
                } else {
                    sharedClass.logger.scriptlog(Level.WARNING, ScriptName, "PlaceholderApi plugin missing or not loaded!", pluginLogger.ORANGE);
                }
            }

            Engine.put("PlaceholderAPI_", placeholderApiJS);
            Engine.eval("""
                const PlaceholderAPI = {
                    registerPlaceholder: function(placeholderPrefix, handler) {
                        PlaceholderAPI_.registerPlaceholder(placeholderPrefix, handler, currentScriptName, scriptEngine);
                    },
                    unregisterPlaceholder: function(placeholderPrefix) {
                        PlaceholderAPI_.unregisterPlaceholder(placeholderPrefix, currentScriptName);
                    },
                    parseString: function(player, text) {
                        return PlaceholderAPI_.parseString(player, text);
                    }
                }
                Object.freeze(PlaceholderAPI);
            """);

            scriptWrapper.addToCleanupMap(ScriptName, () -> {
                placeholderApiJS.unregisterPlaceholders(ScriptName);
            });
        } catch (Exception e) {
            sharedClass.logger.scriptlog(Level.WARNING, ScriptName, "Failed to load script " + e.getMessage(), pluginLogger.ORANGE);
        }
    }
}
