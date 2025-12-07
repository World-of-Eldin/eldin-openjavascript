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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class DiskStorage {

    private final File SAVE_DIR;
    private static final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> privateCache = new ConcurrentHashMap<>();
    private static final Set<String> filesBeingSaved = ConcurrentHashMap.newKeySet();

    public DiskStorage(JavaPlugin Plugin) {
        this.SAVE_DIR = new File(Plugin.getDataFolder(), "saveFiles");
        if (!SAVE_DIR.exists()) {
            SAVE_DIR.mkdirs();
        }
    }

    public void saveAllCaches(boolean async) {
        Runnable task = () -> {
            for (String fileName : cache.keySet()) {
                saveFile(fileName, false, "", true); // Save each file synchronously to avoid thread spam
            }
        };

        if (async) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }

    // TODO: If saveFile operation is running on the file while trying to load it, load from the cache
    public void loadFile(String fileName, boolean async, String scriptName, boolean global) {
        Runnable task = () -> {
            String fullName = (global ? fileName : scriptName + "_" + fileName).replaceAll("[^a-zA-Z0-9._-]", "_");
            File file = new File(SAVE_DIR, fullName + ".dat");
            while (filesBeingSaved.contains(fullName)) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }

            Map<String, String> fileData = new HashMap<>();

            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int sepIndex = line.indexOf('=');
                        if (sepIndex > -1) {
                            String key = line.substring(0, sepIndex);
                            String value = line.substring(sepIndex + 1);
                            fileData.put(key, value);
                        }
                    }
                } catch (IOException e) {
                    sharedClass.logger.log(Level.SEVERE, e.getMessage(), pluginLogger.RED);
                }
            }

            cache.put(fullName, fileData);
            if (!global) {
                privateCache.computeIfAbsent(scriptName, k -> ConcurrentHashMap.newKeySet()).add(fullName);
            }
        };

        if (async) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }

    public void saveCaches(String scriptName) {
        Set<String> files = privateCache.get(scriptName);
        if (files == null) return;

        for (String fullName : files) {
            saveFile(fullName, false, "", true);
        }

        files.clear();
    }

    public void saveFile(String fileName, boolean async, String scriptName, boolean global) {
        Runnable task = () -> {
            String fullName = (global ? fileName : scriptName + "_" + fileName).replaceAll("[^a-zA-Z0-9._-]", "_");
            if (!cache.containsKey(fullName)) return;
            if (!filesBeingSaved.add(fullName)) return; // Already saving

            File file = new File(SAVE_DIR, fullName + ".dat");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, String> entry : cache.get(fullName).entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            } catch (IOException e) {
                sharedClass.logger.log(Level.SEVERE, e.getMessage(), pluginLogger.RED);
            } finally {
                filesBeingSaved.remove(fullName);
                cache.remove(fullName);
            }
        };

        if (async) {
            new Thread(task).start();
        } else {
            task.run();
        }
    }

    public String getValue(String scriptName, boolean global, String fileName, String valueName, String fallbackValue) {
        String fullName = (global ? fileName : scriptName + "_" + fileName).replaceAll("[^a-zA-Z0-9._-]", "_");
        Map<String, String> fileCache = cache.get(fullName);
        return fileCache.getOrDefault(valueName, fallbackValue);
    }

    public void setValue(String scriptName, boolean global, String fileName, String valueName, String value) {
        String fullName = (global ? fileName : scriptName + "_" + fileName).replaceAll("[^a-zA-Z0-9._-]", "_");
        if ("null".equals(value)) {
            Map<String, String> map = cache.get(fullName);
            if (map != null) {
                map.remove(valueName);
            }
        } else {
            cache.computeIfAbsent(fullName, k -> new ConcurrentHashMap<>()).put(valueName, value);
        }
    }
}