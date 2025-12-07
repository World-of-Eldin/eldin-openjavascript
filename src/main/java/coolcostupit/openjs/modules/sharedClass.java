/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.utility.DiskStorage;
import coolcostupit.openjs.utility.configurationUtil;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;

public class sharedClass {
    public static Boolean IsPapiLoaded;
    public static PluginDescriptionFile PluginDescription;
    public static configurationUtil configUtil;
    public static pluginLogger logger;
    public static JavaPlugin plugin;
    public static String Identifier;
    public static DiskStorage DiskStorageApi;
    public static ExecutorService TaskThreadPool;
    public static LibImporterApi LibImporterApi;
}
