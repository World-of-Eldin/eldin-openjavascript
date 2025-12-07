/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import coolcostupit.openjs.logging.pluginLogger;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LibImporterApi {
    private final Map<String, URLClassLoader> cachedLibs = new ConcurrentHashMap<>();
    private final Path libFolderPath;
    private final pluginLogger Logger;

    private WatchService watchService;
    private Thread watcherThread;

    public LibImporterApi() {
        this.Logger = sharedClass.logger;
        this.libFolderPath = sharedClass.plugin.getDataFolder().toPath().resolve("Libs");
        preLoad();
    }

    public void preLoad() {
        File libFolder = libFolderPath.toFile();
        if (!libFolder.exists() && !libFolder.mkdirs()) {
            Logger.log(Level.INFO, "Failed to create Libs folder at: " + libFolder.getAbsolutePath(), pluginLogger.ORANGE);
            return;
        }

        loadAllLibs();

        try {
            watchService = FileSystems.getDefault().newWatchService();
            libFolderPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

            watcherThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            String fileName = event.context().toString();

                            if (!fileName.toLowerCase().endsWith(".jar")) continue;

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                File newJar = libFolderPath.resolve(fileName).toFile();
                                loadLib(newJar);
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                unloadLib(fileName);
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Libs-Watcher");

            watcherThread.setDaemon(true);
            watcherThread.start();

        } catch (IOException e) {
            Logger.log(Level.SEVERE, "Failed to start Libs folder watcher", pluginLogger.RED);
        }
    }

    private void loadAllLibs() {
        File[] jars = libFolderPath.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null) return;

        for (File jar : jars) {
            loadLib(jar);
        }
    }

    public void loadLib(File jarFile) {
        String name = jarFile.getName();

        if (cachedLibs.containsKey(name)) {
            Logger.log(Level.INFO, "Library already loaded: " + name, pluginLogger.LIGHT_BLUE);
            return;
        }

        try {
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jarFile.toURI().toURL()},
                    this.getClass().getClassLoader()
            );
            cachedLibs.put(name, loader);
            Logger.log(Level.INFO, "Loaded lib: " + name, pluginLogger.LIGHT_BLUE);
        } catch (IOException e) {
            Logger.log(Level.SEVERE, "Failed to load lib: " + name + " - " + e.getMessage(), pluginLogger.RED);
        }
    }

    public URLClassLoader getLib(String libName) {
        return cachedLibs.get(libName.endsWith(".jar") ? libName : (libName + ".jar"));
    }

    public void unloadLib(String libName) {
        String fileName = libName.endsWith(".jar") ? libName : (libName + ".jar");
        URLClassLoader loader = cachedLibs.remove(fileName);

        if (loader != null) {
            try {
                loader.close();
                Logger.log(Level.INFO, "Unloaded and removed lib: " + fileName, pluginLogger.GREEN);
            } catch (IOException e) {
                Logger.log(Level.WARNING, "Failed to close loader for lib: " + fileName, pluginLogger.ORANGE);
            }
        } else {
            Logger.log(Level.WARNING, "Tried to unload non-existent lib: " + fileName, pluginLogger.ORANGE);
        }
    }

    public void shutdown() {
        if (watcherThread != null && !watcherThread.isInterrupted()) {
            watcherThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
        cachedLibs.values().forEach(loader -> {
            try {
                loader.close();
            } catch (IOException ignored) {
            }
        });
        cachedLibs.clear();
    }
}