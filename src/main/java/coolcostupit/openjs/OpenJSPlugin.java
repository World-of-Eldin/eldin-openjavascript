/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.*;
import coolcostupit.openjs.pluginbridges.pApiExtension;
import coolcostupit.openjs.utility.*;
import coolcostupit.openjs.logging.OpsLogger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

// Welcome fellow programmer or sane person, you are about to read the cleanest code in this world
// just kidding, the code's structure is ok, but I tried my best ;)
// I am somewhat new to the world of Java, but I have some knowledge from Javascript (why is Java so freaking different)
// Anyway, I haven't put out many comments because I was way too focused or just lazy to write them
// enjoy reading this code, I don't know why I just wrote this, nobody is going to see this anyway (unless I open-sourced this? Why? Hmmm "Open"Js)

public class OpenJSPlugin extends JavaPlugin implements TabExecutor, TabCompleter, Listener {
    public configurationUtil configUtil;
    private pluginLogger pluginLogger;
    private VariableStorage variableStorage;
    private DiskStorage DiskStorageApi;
    private scriptWrapper scriptWrapper;
    private UpdateChecker updateChecker;

    @Override
    public void onEnable() {
        Server server = getServer();
        PluginManager pluginManager = server.getPluginManager();

        this.configUtil = new configurationUtil(this);
        this.pluginLogger = new pluginLogger(this, configUtil);
        this.variableStorage = new VariableStorage(this);
        this.DiskStorageApi = new DiskStorage(this);

        // Validate that Javet (V8) engine is available
        try {
            com.caoccao.javet.interop.V8Runtime testRuntime = ScriptEngine.getEngine();
            testRuntime.close();
        } catch (Exception e) {
            pluginLogger.log(Level.SEVERE, "Failed to initialize Javet V8 JavaScript engine: " + e.getMessage(), pluginLogger.RED);
            pluginLogger.log(Level.SEVERE, "Make sure you're running on a supported platform (Linux x64/ARM64, Windows x64, macOS x64/ARM64)", pluginLogger.RED);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sharedClass.configUtil = configUtil;
        sharedClass.PluginDescription = this.getDescription();
        sharedClass.IsPapiLoaded = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        sharedClass.logger = pluginLogger;
        sharedClass.plugin = this;
        sharedClass.Identifier = this.getName().toLowerCase();
        sharedClass.DiskStorageApi = DiskStorageApi;
        sharedClass.LibImporterApi = new LibImporterApi();

        if (sharedClass.IsPapiLoaded) {
            new pApiExtension().register();
        }

        this.scriptWrapper = new scriptWrapper(this, configUtil);
        this.updateChecker = new UpdateChecker(this, this.pluginLogger, this.configUtil);

        JavascriptHelper.updateSource();
        updateChecker.startChecking();
        scriptWrapper.loadDisabledScripts();
        scriptWrapper.checkDisabledScripts();
        saveDefaultConfig();
        configUtil.loadBufferFromConfig();
        scriptWrapper.loadScripts();

        // Default config values
        configUtil.getConfigFromBuffer("PrintScriptActivations", true);
        configUtil.getConfigFromBuffer("UseCustomInterpreter", true);
        configUtil.getConfigFromBuffer("LoadCustomEventsHandler", true);
        configUtil.getConfigFromBuffer("LogPlaceHolderActivity", true);
        configUtil.getConfigFromBuffer("LogCustomCommandsActivity", true);
        configUtil.getConfigFromBuffer("AllowBstats", true);
        configUtil.getConfigFromBuffer("LoadCustomScheduler", true);
        configUtil.getConfigFromBuffer("UpdateNotifications", true);
        configUtil.getConfigFromBuffer("AllowFeatureFlags", true);
        configUtil.getConfigFromBuffer("BroadcastToOps", true);
        configUtil.saveBufferToConfig();

        if (configUtil.getConfigFromBuffer("AllowBstats", true)) {
            // here ya go chads, you can opt-out bstats if u want to :)
            Metrics metrics = new Metrics(this, 22268);
        };

        getCommand("oj").setExecutor(this);
        getCommand("oj").setTabCompleter(this);
        getCommand("openjavascript").setExecutor(this);
        getCommand("openjavascript").setTabCompleter(this);

        pluginManager.registerEvents(this, this);

        pluginLogger.log(Level.INFO, "[<------------------------------->]", pluginLogger.BLUE);
        pluginLogger.log(Level.INFO, "      [Loaded OpenJavascript]", pluginLogger.LIGHT_BLUE);
        pluginLogger.log(Level.INFO, "Version: " + sharedClass.PluginDescription.getVersion(), pluginLogger.LIGHT_BLUE);
        pluginLogger.log(Level.INFO, "Author: " + sharedClass.PluginDescription.getAuthors().toString().substring(1, sharedClass.PluginDescription.getAuthors().toString().length() - 1), pluginLogger.LIGHT_BLUE);
        pluginLogger.log(Level.INFO, "Java Version: " + System.getProperty("java.version"), pluginLogger.LIGHT_BLUE);
        if (FoliaSupport.isFolia() == true) {
            pluginLogger.log(Level.INFO, "Folia Support: true", pluginLogger.LIGHT_BLUE);
        }
        if (sharedClass.IsPapiLoaded) {
            pluginLogger.log(Level.INFO, "PlaceholderApi Support: true", pluginLogger.LIGHT_BLUE);
        }
        pluginLogger.log(Level.INFO, "[<------------------------------->]", pluginLogger.BLUE);
    }

    @Override
    public void onDisable() {
        pluginLogger.log(Level.INFO, "[<---------------------------------->]", coolcostupit.openjs.logging.pluginLogger.BLUE);
        pluginLogger.log(Level.INFO, "      [OpenJavascript shutdown]", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        pluginLogger.log(Level.INFO, "Un-registering all listeners...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        scriptWrapper.unregisterAllListeners();
        pluginLogger.log(Level.INFO, "Un-registering all tasks...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        scriptWrapper.unregisterAllTasks();
        pluginLogger.log(Level.INFO, "Un-registering all script commands...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        scriptWrapper.unregisterAllScriptCommands();
        pluginLogger.log(Level.INFO, "Un-loading all scripts...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        sharedClass.TaskThreadPool.shutdown();
        scriptWrapper.executorService.shutdown();
        scriptWrapper.unloadAllScripts();
        sharedClass.LibImporterApi.shutdown();
        pluginLogger.log(Level.INFO, "Storing memory variables...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        variableStorage.saveVariables();
        pluginLogger.log(Level.INFO, "Saving disk storage files...", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        DiskStorageApi.saveAllCaches(false);
        pluginLogger.log(Level.INFO, "[OpenJavascript shutdown successfully]", coolcostupit.openjs.logging.pluginLogger.LIGHT_BLUE);
        pluginLogger.log(Level.INFO, "[<---------------------------------->]", coolcostupit.openjs.logging.pluginLogger.BLUE);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (configUtil.getConfigFromBuffer("UpdateNotifications", true)) {
            if (updateChecker.UpdatesAvailable() && OpsLogger.hasPerm(player)) {
                player.sendMessage(chatColors.GRAY+"-----------------------------------------------------");
                player.sendMessage(chatColors.ORANGE+"A new version of OpenJS is available!");
                player.sendMessage(chatColors.ORANGE+"Current version: " + chatColors.RED + updateChecker.CurrentVersion);
                player.sendMessage(chatColors.ORANGE+"Latest version: " + chatColors.GREEN + updateChecker.LatestVersion);
                player.sendMessage(chatColors.ORANGE+"Download it here: " + chatColors.LIGHT_BLUE + "https://modrinth.com/plugin/openjavascript/version/latest");
                player.sendMessage(chatColors.GRAY+"-----------------------------------------------------");
            }
        }
    }

    private void list_enabled_scripts(CommandSender sender) {
        sender.sendMessage(chatColors.GREEN + "Enabled scripts:");
        if (scriptWrapper.activeFiles.isEmpty()) {
            sender.sendMessage(chatColors.GREEN + "- There are no enabled scripts.");
        } else {
            for (String script : scriptWrapper.activeFiles) {
                sender.sendMessage(chatColors.GREEN + "- " + script);
            }
        }
    }

    private void list_disabled_scripts(CommandSender sender) {
        sender.sendMessage(chatColors.RED + "Disabled scripts:");
        if (scriptWrapper.disabledScripts.isEmpty()) {
            sender.sendMessage(chatColors.RED + "- There are no disabled scripts.");
        } else {
            for (String script : scriptWrapper.disabledScripts) {
                sender.sendMessage(chatColors.RED + "- " + script);
            }
        }
    }

    private void list_unloaded_scripts(CommandSender sender) {
        sender.sendMessage(chatColors.DARK_RED + "Scripts that are not loaded yet:");
        List<String> notLoadedScripts = scriptWrapper.getNotLoadedScripts();
        if (notLoadedScripts.isEmpty()) {
            sender.sendMessage(chatColors.DARK_RED + "- There are no scripts that haven't been loaded yet.");
        } else {
            for (String script : notLoadedScripts) {
                sender.sendMessage(chatColors.DARK_RED + "- " + script);
            }
        }
    }

    private void sendUsageMessage(CommandSender sender, String label) {
        sender.sendMessage(chatColors.LIGHT_PURPLE + "Usage: /" + label + " <command>");
        sender.sendMessage(chatColors.LIGHT_PURPLE + "Available commands:");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " version               " + chatColors.GRAY + "» Shows the plugin version");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " wiki | documentation  " + chatColors.GRAY + "» Shows the documentation link");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " download              " + chatColors.GRAY + "» Shows the latest download link");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " reload [script]       " + chatColors.GRAY + "» Reloads a specific script or all scripts");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " load <script>         " + chatColors.GRAY + "» Loads a script by name");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " enable <script>       " + chatColors.GRAY + "» Enables a disabled script");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " disable <script>      " + chatColors.GRAY + "» Disables an enabled script");
        sender.sendMessage(chatColors.LIGHT_PURPLE + " - /" + label + " list <type>           " + chatColors.GRAY + "» Lists scripts by type: enabled, disabled, or not_loaded");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendUsageMessage(sender, label);
            return true;
        } else if ("help".equalsIgnoreCase(args[0])) {
            sendUsageMessage(sender, label);
            return true;
        } else if ("version".equalsIgnoreCase(args[0])) {
            sender.sendMessage(chatColors.LIGHT_BLUE + "Version: " + sharedClass.PluginDescription.getVersion());
            return true;
        } else if ("documentation".equalsIgnoreCase(args[0]) | "wiki".equalsIgnoreCase(args[0])) {
            sender.sendMessage(pluginLinks.getLink("wiki"));
            return true;
        } else if ("download".equalsIgnoreCase(args[0])) {
            sender.sendMessage(pluginLinks.getLink("download"));
            return true;
        } else if (args.length == 1 && "list".equalsIgnoreCase(args[0])) {
            list_enabled_scripts(sender);
            list_disabled_scripts(sender);
            list_unloaded_scripts(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "reload":
                if (args.length < 2) {
                    configUtil.reloadConfigBuffer();
                    JavascriptHelper.updateSource();
                    scriptWrapper.loadScripts();
                    sender.sendMessage(chatColors.GREEN+"All scripts and the config have been reloaded.");
                    return true;
                } else {
                    String scriptToReload = args[1];
                    if (scriptWrapper.isJavascriptFileActive(scriptToReload)) {
                        File scriptFile = new File(getDataFolder(), "scripts/" + scriptToReload);
                        if (scriptFile.exists()) {
                            scriptWrapper.loadScript(scriptFile, true);
                            sender.sendMessage(chatColors.GREEN+"Script " + scriptToReload + " has been reloaded.");
                            return  true;
                        } else {
                            sender.sendMessage(chatColors.RED+"Can't reload " + scriptToReload + " because it does not exist.");
                        }
                    } else {
                        sender.sendMessage(chatColors.RED+"Can't reload " + scriptToReload + " because it is not enabled.");
                    }
                }
                return true;
            case "enable":
                if (args.length < 2) {
                    sender.sendMessage(chatColors.LIGHT_PURPLE+"Usage: /" + label + " " + subCommand + " <scriptName>");
                    return true;
                }
                String scriptToEnable = args[1];
                if (scriptWrapper.disabledScripts.contains(scriptToEnable)) {
                    scriptWrapper.disabledScripts.remove(scriptToEnable);
                    scriptWrapper.saveDisabledScripts();
                    File scriptFile = new File(getDataFolder(), "scripts/" + scriptToEnable);
                    if (scriptFile.exists()) {
                        scriptWrapper.loadScript(scriptFile, true);
                        sender.sendMessage(chatColors.GREEN+"Script " + scriptToEnable + " enabled.");
                        return  true;
                    } else {
                        sender.sendMessage(chatColors.RED+"Script " + scriptToEnable + " not found.");
                    }
                } else {
                    sender.sendMessage(chatColors.RED+"Script " + scriptToEnable + " is not disabled.");
                }
                return true;
            case "disable":
                if (args.length < 2) {
                    sender.sendMessage(chatColors.LIGHT_PURPLE+"Usage: /" + label + " " + subCommand + " <scriptName>");
                    return true;
                }
                String scriptToDisable = args[1];
                if (scriptWrapper.isJavascriptFileActive(scriptToDisable)) {
                    scriptWrapper.activeFiles.remove(scriptToDisable);
                    scriptWrapper.disabledScripts.add(scriptToDisable);
                    scriptWrapper.unloadScript(scriptToDisable);
                    scriptWrapper.saveDisabledScripts();
                    sender.sendMessage(chatColors.RED+"Script " + scriptToDisable + " disabled.");
                    return true;
                } else {
                    sender.sendMessage(chatColors.RED+"Script " + scriptToDisable + " is not active.");
                }
                return true;
            case "load":
                if (args.length < 2) {
                    sender.sendMessage(chatColors.LIGHT_PURPLE+"Usage: /" + label + " " + subCommand + " <scriptName>");
                    return true;
                }
                String scriptToLoad = args[1];

                if (scriptWrapper.disabledScripts.contains(scriptToLoad)) {
                    sender.sendMessage(chatColors.RED+"Unable to load a disabled script. Use command /"+label+" enable "+scriptToLoad+" to load it.");
                    return true;
                }

                File scriptFile = new File(getDataFolder(), "scripts/" + scriptToLoad);
                if (scriptFile.exists()) {
                    scriptWrapper.loadScript(scriptFile, true);
                    sender.sendMessage(chatColors.GREEN+"Script " + scriptToLoad + " loaded.");
                    return  true;
                } else {
                    sender.sendMessage(chatColors.RED+"Script " + scriptToLoad + " not found.");
                }
                return true;
            case "list":
                String commandToExecute = args[1];
                if (commandToExecute.equalsIgnoreCase("enabled")) {
                    list_enabled_scripts(sender);
                } else if (commandToExecute.equalsIgnoreCase("disabled")) {
                    list_disabled_scripts(sender);
                } else if (commandToExecute.equalsIgnoreCase("not_loaded")) {
                    list_unloaded_scripts(sender);
                } else {
                    sender.sendMessage(chatColors.RED+"Usage /" + label + " list *(enabled/disabled/not_loaded)");
                }
                return  true;
            default:
                sender.sendMessage(chatColors.RED+"Unknown command.");
                return true;
        }
    }


    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("version".startsWith(args[0].toLowerCase())) {
                completions.add("version");
            }
            if ("wiki".startsWith(args[0].toLowerCase())) {
                completions.add("wiki");
            }
            if ("documentation".startsWith(args[0].toLowerCase())) {
                completions.add("documentation");
            }
            if ("download".startsWith(args[0].toLowerCase())) {
                completions.add("download");
            }
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if ("enable".startsWith(args[0].toLowerCase())) {
                completions.add("enable");
            }
            if ("disable".startsWith(args[0].toLowerCase())) {
                completions.add("disable");
            }
            if ("load".startsWith(args[0].toLowerCase())) {
                completions.add("load");
            }
            if ("help".startsWith(args[0].toLowerCase())) {
                completions.add("help");
            }
            if ("list".startsWith(args[0].toLowerCase())) {
                completions.add("list");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("enable")) {
                for (String script : scriptWrapper.disabledScripts) {
                    if (script.startsWith(args[1].toLowerCase())) {
                        completions.add(script);
                    }
                }
            } else if (args[0].equalsIgnoreCase("disable")) {
                for (String script : scriptWrapper.activeFiles) {
                    if (script.startsWith(args[1].toLowerCase())) {
                        completions.add(script);
                    }
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                for (String script : scriptWrapper.activeFiles) {
                    if (script.startsWith(args[1].toLowerCase())) {
                        completions.add(script);
                    }
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                completions.add("enabled");
                completions.add("disabled");
                completions.add("not_loaded");
            } else if (args[0].equalsIgnoreCase("load")) {
                File scriptsFolder = new File(getDataFolder(), "scripts");
                if (scriptsFolder.exists() && scriptsFolder.isDirectory()) {
                    for (File scriptFile : Objects.requireNonNull(scriptsFolder.listFiles())) {
                        if (scriptFile.isFile() && scriptFile.getName().endsWith(".js") && !scriptWrapper.activeFiles.contains(scriptFile.getName()) && !scriptWrapper.disabledScripts.contains(scriptFile.getName())) {
                            if (scriptFile.getName().startsWith(args[1].toLowerCase())) {
                                completions.add(scriptFile.getName());
                            }
                        }
                    }
                }
            }
        }
        return completions;
    }
}
