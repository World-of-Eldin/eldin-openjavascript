/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import java.io.File;
import java.nio.file.Path;

/**
 * Utility class for handling script paths and identifiers.
 * Provides methods to convert between File objects and normalized path strings
 * for scripts that may be located in subfolders.
 */
public class ScriptPathUtils {

    /**
     * Converts a script File to its unique identifier (relative path from scripts folder).
     * Always uses forward slashes regardless of OS for consistency.
     *
     * @param scriptsFolder The base scripts folder
     * @param scriptFile The script file to get identifier for
     * @return Normalized path using forward slashes, e.g. "utilities/helper.js"
     */
    public static String getScriptIdentifier(File scriptsFolder, File scriptFile) {
        Path scriptsPath = scriptsFolder.toPath().toAbsolutePath().normalize();
        Path scriptPath = scriptFile.toPath().toAbsolutePath().normalize();

        Path relativePath = scriptsPath.relativize(scriptPath);

        // Always use forward slashes for consistency across platforms
        return relativePath.toString().replace(File.separator, "/");
    }

    /**
     * Converts a script identifier back to a File object.
     *
     * @param scriptsFolder The base scripts folder
     * @param identifier The script identifier (e.g. "utilities/helper.js")
     * @return File object for the script
     */
    public static File identifierToFile(File scriptsFolder, String identifier) {
        // Convert forward slashes to system-specific separator if needed
        String systemPath = identifier.replace("/", File.separator);
        return new File(scriptsFolder, systemPath);
    }

    /**
     * Validates that a path identifier is safe (no directory traversal attacks).
     *
     * @param identifier The script identifier to validate
     * @return true if the path is safe, false otherwise
     */
    public static boolean isValidScriptPath(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }

        // Reject any path traversal attempts
        if (identifier.contains("..")) {
            return false;
        }

        // Reject absolute paths
        if (identifier.startsWith("/") || identifier.startsWith("\\")) {
            return false;
        }

        // Reject Windows drive letters
        if (identifier.matches("^[A-Za-z]:.*")) {
            return false;
        }

        return true;
    }

    /**
     * Gets a display name for user-facing messages.
     * Currently just returns the identifier as-is, but provides extension point
     * for future formatting changes.
     *
     * @param identifier The script identifier
     * @return Display name for the script
     */
    public static String getDisplayName(String identifier) {
        return identifier;
    }

    /**
     * Converts a script identifier to a safe filename for DiskStorage.
     * Replaces path separators with underscores to create flat filenames.
     *
     * @param identifier The script identifier (e.g. "utils/test.js")
     * @return Safe filename (e.g. "utils_test.js")
     */
    public static String toSafeFileName(String identifier) {
        return identifier.replace("/", "_").replace("\\", "_");
    }
}
