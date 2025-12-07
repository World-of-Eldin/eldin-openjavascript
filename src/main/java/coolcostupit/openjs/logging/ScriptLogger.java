/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptLogger {
    private final Logger logger;
    private final String scriptName;

    public ScriptLogger(Logger logger, String scriptName) {
        this.logger = logger;
        this.scriptName = scriptName;
    }

    @SuppressWarnings("unused")
    private void log(Level level, String message) {
        logger.log(level, "[" + scriptName + "] " + message);
    }
    @SuppressWarnings("unused")
    public void warn(String message) {
        log(Level.WARNING, message);
    }
    @SuppressWarnings("unused")
    public void info(String message) {
        log(Level.INFO, message);
    }
    @SuppressWarnings("unused")
    public void error(String message) {
        log(Level.SEVERE, message);
    }
}
