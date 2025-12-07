/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FlagInterpreter {

    private static final int SWELL_LIMIT = 10;

    public static boolean hasFlag(File scriptFile, String flag) {
        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            int swell = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("//")) {
                    swell++;
                } else if (line.startsWith("//!" + flag)) {
                    return true;
                } else {
                    swell++;
                }

                if (swell >= SWELL_LIMIT) break;
            }
        } catch (IOException e) {
            sharedClass.logger.scriptlog(Level.WARNING, scriptFile.getName(), "Error during flag loading: " + e.getMessage(), pluginLogger.ORANGE);
        }
        return false;
    }

    public static List<String> getFlags(File scriptFile) {
        List<String> flags = new ArrayList<>();
        int swell = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("//!")) {
                    flags.add(line.substring(3).trim());
                    swell = 0; // Reset on valid flag
                } else if (line.isEmpty() || line.startsWith("//")) {
                    swell++;
                } else {
                    swell++;
                }

                if (swell >= SWELL_LIMIT) break;
            }
        } catch (IOException e) {
            sharedClass.logger.scriptlog(Level.WARNING, scriptFile.getName(), "Error during flag loading: " + e.getMessage(), pluginLogger.ORANGE);
        }

        return flags;
    }
}
