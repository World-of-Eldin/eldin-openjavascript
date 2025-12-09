/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.logging;

import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import coolcostupit.openjs.utility.chatColors;
import coolcostupit.openjs.utility.configurationUtil;

public class pluginLogger {

    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[38;5;49m";
    public static final String BLUE = "\u001B[34m";
    public static final String LIGHT_BLUE = "\u001B[38;5;81m";
    public static final String ORANGE = "\u001B[38;5;214m";
    public static final String YELLOW = "\u001B[33m";
    public static final String CYAN = "\u001B[36m";

    // Special indicators
    public static final String yieldKill = "_殺了我ͶͶ";

    private final configurationUtil configUtil;
    private final Logger logger;

    public pluginLogger(JavaPlugin plugin, configurationUtil configUtil) {
        this.logger = plugin.getLogger();
        this.configUtil = configUtil;
    }

    public void log(Level level, String message, String colorCode) {
        logger.log(level, colorCode + message + RESET);
        if (configUtil.getConfigFromBuffer("BroadcastToOps", true) && level == Level.SEVERE || level == Level.WARNING) {
            OpsLogger.LogToOps(Collections.singletonList(chatColors.RED + message));
        }
    }

    public void scriptlog(Level level, String scriptName, String message, String colorCode) {
        if (message != null && message.contains(yieldKill)) { // special Unicode's so that scripts won't silently call this
            this.log(Level.INFO, "[" + scriptName + "] stopped executing", LIGHT_BLUE);
        } else {
            this.log(level, "[" + scriptName + "] " + message, colorCode);
        }
    }

    public void internalException(String scriptName, String exception) {
        this.scriptlog(Level.WARNING, scriptName, exception, RED);
    }
}
