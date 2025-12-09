/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;

import coolcostupit.openjs.events.ScriptLoadedEvent;
import coolcostupit.openjs.events.ScriptUnloadedEvent;
import coolcostupit.openjs.logging.ScriptLogger;
import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.pluginbridges.BridgeLoader;
import coolcostupit.openjs.utility.FlagInterpreter;
import coolcostupit.openjs.utility.JavascriptHelper;
import coolcostupit.openjs.utility.ScriptPathUtils;
import coolcostupit.openjs.utility.VariableStorage;
import coolcostupit.openjs.utility.chatColors;
import coolcostupit.openjs.utility.configurationUtil;

// this is the main stuff, but I haven't added many to no comments because I was way too focused when coding all that
public class scriptWrapper {
    private boolean scriptsReady = false;
    private boolean hasInit = false;
    private final Map<String, List<Listener>> eventListenersMap = new HashMap<>();
    public final Map<String, List<Integer>> scriptTasksMap = new HashMap<>();
    private final Map<String, Future<?>> scriptFutures = new HashMap<>();
    private final Map<String, V8Runtime> scriptEngines = new HashMap<>();
    private final Map<String, List<Command>> scriptCommands = new HashMap<>();
    private static final Map<String, List<Runnable>> cleanUpMethods = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private final File disabledScriptsFile;
    private final pluginLogger Logger;
    private final PublicVarManager PublicVarManager;
    private final configurationUtil configUtil;
    private final VariableStorage variableStorage;
    public final List<String> disabledScripts = new ArrayList<>();
    public final List<String> activeFiles = new ArrayList<>();
    public final List<String> runningScripts = new ArrayList<>();
    public final ExecutorService executorService;
    private final scriptTaskerApi taskApi;

    public scriptWrapper(JavaPlugin plugin, configurationUtil configUtil) {
        this.plugin = plugin;
        this.Logger = new pluginLogger(plugin, configUtil);
        this.PublicVarManager = new PublicVarManager();
        this.configUtil = configUtil;
        this.variableStorage = new VariableStorage(plugin);
        this.executorService = Executors.newCachedThreadPool();
        this.taskApi = new scriptTaskerApi(this);

        // Javet is configured with V8 backend in ScriptEngine.java
        // Supports full ECMAScript 2024+ including async/await, optional chaining, BigInt, etc.

        // Initialize script system on first use
        if (!hasInit) {
            hasInit = true;
            FoliaSupport.ScheduleTask(plugin, () -> scriptsReady = true, 20L);
            //plugin.getServer().getScheduler().runTaskLater(plugin, () -> scriptsReady = true, 20L);
        }

        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        boolean scriptsFolderCreated = scriptsFolder.mkdirs(); // Check the return value

        disabledScriptsFile = new File(plugin.getDataFolder(), "disabledscripts.json");
        if (!disabledScriptsFile.exists()) {
            try {
                boolean fileCreated = disabledScriptsFile.createNewFile(); // Check the return value
                if (fileCreated) {
                    try (FileWriter writer = new FileWriter(disabledScriptsFile)) {
                        writer.write("[]");
                    }
                }
            } catch (IOException e) {
                Logger.log(Level.SEVERE, "Failed to create disabledscripts.json." + e.getMessage(), pluginLogger.RED);
            }
        }

        if (!scriptsFolderCreated && !scriptsFolder.exists()) {
            Logger.log(Level.WARNING, "Failed to create scripts folder.", pluginLogger.ORANGE);
        }
    }

    public boolean isJavascriptFileActive(String fileName) {
        return activeFiles.contains(fileName);
    }

    public boolean isJavascriptFileRunning(String fileName) {
        return runningScripts.contains(fileName);
    }

    public static void addToCleanupMap(String scriptName, Runnable method) {
        cleanUpMethods.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(method);
    }

    public List<Listener> getEventListenersFromScript(String scriptName) {
        return eventListenersMap.getOrDefault(scriptName, null);
    }

    public void unregisterListener(Listener listener, String scriptName) {
        HandlerList.unregisterAll(listener);
        List<Listener> listeners = eventListenersMap.get(scriptName);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                eventListenersMap.remove(scriptName);
            }
        }
    }

    public void unregisterListenersFromScript(String scriptName) {
        List<Listener> activeListeners = getEventListenersFromScript(scriptName);
        if (activeListeners != null) {
            List<Listener> listenersToRemove = new ArrayList<>(activeListeners);
            for (Listener listener : listenersToRemove) {
                unregisterListener(listener, scriptName);
            }
        }
    }

    public void unregisterTasksFromScript(String scriptName) {
        List<Integer> taskIds = scriptTasksMap.get(scriptName);
        if (taskIds != null) {
            for (int taskId : taskIds) {
                FoliaSupport.CancelTask(taskId);
            }
            scriptTasksMap.remove(scriptName);
        }
    }

    private static void invokeScriptCleanup(String scriptName) {
        List<Runnable> tasks = cleanUpMethods.remove(scriptName);
        if (tasks != null) {
            for (Runnable cleanup : tasks) {
                try {
                    cleanup.run();
                } catch (Exception e) {
                    sharedClass.logger.scriptlog(Level.INFO, scriptName, "External plugin methods cleanup failed: " + e.getMessage(), pluginLogger.ORANGE);
                }
            }
        }
    }

    public void unloadAllScripts() {
        for (String scriptName : new ArrayList<>(activeFiles)) {
            unloadScript(scriptName);
        }
    }

    public void unregisterAllTasks() {
        for (List<Integer> taskIds : scriptTasksMap.values()) {
            for (int taskId : taskIds) {
                FoliaSupport.CancelTask(taskId);
            }
        }
        scriptTasksMap.clear();
    }

    public void loadDisabledScripts() {
        try (FileReader reader = new FileReader(disabledScriptsFile)) {
            JSONParser parser = new JSONParser();
            JSONArray jsonArray = (JSONArray) parser.parse(reader);
            for (Object obj : jsonArray) {
                disabledScripts.add((String) obj);
            }
        } catch (IOException | ParseException e) {
            Logger.log(Level.SEVERE, "Failed to load disabled scripts." + e.getMessage(), pluginLogger.RED);
        }
    }

    public void unregisterAllListeners() {
        for (Map.Entry<String, List<Listener>> entry : eventListenersMap.entrySet()) {
            List<Listener> listeners = entry.getValue();
            for (Listener listener : listeners) {
                HandlerList.unregisterAll(listener);
            }
        }
        eventListenersMap.clear();
    }

    public CommandMap getCommandMap() {
        CommandMap commandMap = null;

        try {
            Field f = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);

            commandMap = (CommandMap) f.get(Bukkit.getPluginManager());
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            Logger.log(Level.SEVERE, "Failed to load CommandMap: " + e.getMessage(), pluginLogger.RED);
        }

        return commandMap;
    }

    private void removeCommandFromKnownCommands(String commandName) throws Exception {
        CommandMap commandMap = getCommandMap();

        Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
        knownCommandsField.setAccessible(true);

        Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
        knownCommands.remove(commandName); // Remove the command
    }

    private void invokeSyncCommands() {
        try {
            Class<?> serverClass = Bukkit.getServer().getClass();
            Method method = getMethod(serverClass, "syncCommands");
            method.setAccessible(true);
            method.invoke(Bukkit.getServer());
            method.setAccessible(false);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Logger.log(Level.SEVERE, "Failed to sync commands: " + e.getMessage(), pluginLogger.RED);
        }
    }

    private Method getMethod(Class<?> clazz, String methodName) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getMethod(superClass, methodName);
            }
        }
    }

    // Unregister all commands for a specific script
    public void unregisterCommands(String scriptName) {
        List<Command> commands = scriptCommands.remove(scriptName);
        if (commands != null) {
            try {
                CommandMap commandMap = getCommandMap();

                for (Command dynamicCommand : commands) {
                    // Unregister the command using CommandMap directly
                    removeCommandFromKnownCommands(dynamicCommand.getName());
                    boolean Unregistered = dynamicCommand.unregister(commandMap);
                    if (Unregistered) {
                        if (configUtil.getConfigFromBuffer("LogCustomCommandsActivity", true)) {
                            Logger.scriptlog(Level.INFO, scriptName, "Unregistered command: " + dynamicCommand.getName(), pluginLogger.GREEN);
                        }
                        invokeSyncCommands();
                    } else {
                        Logger.scriptlog(Level.INFO, scriptName, "Failed to unregister command: " + dynamicCommand.getName(), pluginLogger.ORANGE);
                    }
                }
            } catch (Exception e) {
                Logger.scriptlog(Level.SEVERE, scriptName, "Failed to unregister commands: " + e, pluginLogger.RED);
                Logger.scriptlog(Level.SEVERE, scriptName, e.getMessage(), pluginLogger.RED);
            }
        }
    }


    // Unregister all dynamically registered commands
    public void unregisterAllScriptCommands() {
        try {
            for (String scriptName : new ArrayList<>(scriptCommands.keySet())) {
                unregisterCommands(scriptName);
            }
            scriptCommands.clear();
        } catch (Exception e) {
            Logger.log(Level.SEVERE, "Failed to unregister all script commands: " + e.getMessage(), pluginLogger.ORANGE);
        }
    }

    public void saveDisabledScripts() {
        try (FileWriter writer = new FileWriter(disabledScriptsFile)) {
            JSONArray jsonArray = new JSONArray();
            jsonArray.addAll(disabledScripts);
            writer.write(jsonArray.toJSONString());
        } catch (IOException e) {
            Logger.log(Level.SEVERE, "Failed to save disabled scripts." + e.getMessage(), pluginLogger.RED);
        }
    }

    public void checkDisabledScripts() {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        List<String> scriptsInFolder = new ArrayList<>();

        if (scriptsFolder.exists() && scriptsFolder.isDirectory()) {
            // Use recursive scan to find all scripts
            List<File> allScripts = getAllScriptFiles(scriptsFolder);
            for (File scriptFile : allScripts) {
                String scriptId = ScriptPathUtils.getScriptIdentifier(scriptsFolder, scriptFile);
                scriptsInFolder.add(scriptId);
            }
        }

        boolean modified = false;
        Iterator<String> iterator = disabledScripts.iterator();
        while (iterator.hasNext()) {
            String scriptId = iterator.next();
            if (!scriptsInFolder.contains(scriptId)) {
                iterator.remove();
                modified = true;
                Logger.log(Level.INFO, "Removed non-existent script " + scriptId + " from disabled scripts list.", pluginLogger.BLUE);
            }
        }

        if (modified) {
            saveDisabledScripts();
        }
    }

    public void unloadScript(String scriptName) {
        if (!runningScripts.contains(scriptName)) {
            return;
        }

        invokeScriptCleanup(scriptName);
        taskApi.clearListeners(scriptName);
        unregisterListenersFromScript(scriptName);
        unregisterCommands(scriptName);
        unregisterTasksFromScript(scriptName);
        sharedClass.DiskStorageApi.saveCaches(scriptName); // ASYNC ?=> yields

        Future<?> future = scriptFutures.remove(scriptName);
        if (future != null) {
            future.cancel(true);
        }

        V8Runtime engine = scriptEngines.remove(scriptName);
        runningScripts.remove(scriptName);
        if (engine != null) {
            try (V8ValueObject globalObject = engine.getGlobalObject()) {
                if (globalObject.has("_unloadThis")) {
                    try (V8ValueFunction unloadFn = globalObject.get("_unloadThis")) {
                        if (!unloadFn.isUndefined()) {
                            unloadFn.callVoid(null);
                        }
                    }
                }
            } catch (JavetException e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Failed to call cleanup function: " + e.getMessage(), pluginLogger.RED);
            }

            // Javet cleanup: notify GC and close runtime
            engine.lowMemoryNotification();
            try {
                engine.close();
            } catch (JavetException e) {
                Logger.scriptlog(Level.SEVERE, scriptName, "Failed to close V8Runtime: " + e.getMessage(), pluginLogger.RED);
            }
        }

        if (plugin.isEnabled()) {
            // Folia fallback
            FoliaSupport.runTaskSynchronously(plugin, () -> plugin.getServer().getPluginManager().callEvent(new ScriptUnloadedEvent(scriptName)));
            //plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(new ScriptUnloadedEvent(scriptName)));
        }
    }

    public String preprocessScript(File scriptFile, V8Runtime scriptEngine, V8ValueObject globalObject) throws IOException, JavetException {
        if (!configUtil.getConfigFromBuffer("UseCustomInterpreter", true)) {
            return new String(Files.readAllBytes(scriptFile.toPath()));
        }

        StringBuilder scriptContent = new StringBuilder();
        List<String> imports = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("//!import ")) {
                    String importLine = line.substring(9).trim();
                    imports.add(importLine);
                } else {
                    scriptContent.append(line).append("\n");
                }
            }
        }

        StringBuilder finalScript = new StringBuilder();

        // Use the passed-in global object reference - don't get a new one!
        for (String importStatement : imports) {
            try {
                Class<?> clazz = Class.forName(importStatement);
                String simpleName = clazz.getSimpleName();
                globalObject.set(simpleName, clazz);
            } catch (ClassNotFoundException e) {
                Logger.scriptlog(Level.WARNING, scriptFile.getName(), "Class not found for import: " + importStatement, pluginLogger.ORANGE);
            } catch (JavetException e) {
                Logger.scriptlog(Level.WARNING, scriptFile.getName(), "Failed to set import for " + importStatement + ": " + e.getMessage(), pluginLogger.ORANGE);
            }
        }

        finalScript.append(scriptContent);

        // Internal exception catcher
        return """
            try {
                %s
            } catch (e) {
                _internalPluginLogger.internalException(currentScriptName, "Exception: " + e);
                if (e && e.stack)  _internalPluginLogger.internalException(currentScriptName, "Stack: " + e.stack);
            }
            """.formatted(finalScript.toString());
    }

    /**
     * Recursively discovers all .js files in the scripts folder and subfolders
     * @param directory Starting directory to scan
     * @return List of all script files found (including subdirectories)
     */
    private List<File> getAllScriptFiles(File directory) {
        List<File> scripts = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files == null) {
            return scripts; // Directory doesn't exist or I/O error
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // Recurse into subdirectory
                scripts.addAll(getAllScriptFiles(file));
            } else if (file.isFile() && file.getName().endsWith(".js")) {
                scripts.add(file);
            }
            // Ignore other file types
        }

        return scripts;
    }

    public List<String> getNotLoadedScripts() {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        List<String> notLoadedScripts = new ArrayList<>();
        if (scriptsFolder.exists() && scriptsFolder.isDirectory()) {
            // Use recursive scan to find all scripts
            List<File> allScripts = getAllScriptFiles(scriptsFolder);
            for (File scriptFile : allScripts) {
                String scriptId = ScriptPathUtils.getScriptIdentifier(scriptsFolder, scriptFile);
                if (!activeFiles.contains(scriptId) && !disabledScripts.contains(scriptId)) {
                    notLoadedScripts.add(scriptId);
                }
            }
        }
        return notLoadedScripts;
    }

    public static class ScriptLoadResult { // this is ancient from 1.0.0 Alpha, may need to look into it
        private final boolean success;
        private final String message;

        public ScriptLoadResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public ScriptLoadResult loadScript(File scriptFile, boolean calledFromScript) {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        String scriptId = ScriptPathUtils.getScriptIdentifier(scriptsFolder, scriptFile);

        if (scriptFile.isFile() && scriptFile.getName().endsWith(".js") && !disabledScripts.contains(scriptId)) {
            if (calledFromScript) {
                if (!hasInit || !scriptsReady) {
                    return new ScriptLoadResult(false, "Do not manually load scripts while they are being initialized!");
                }
            } else {
                if (configUtil.getConfigFromBuffer("AllowFeatureFlags", true)) {
                    if (FlagInterpreter.hasFlag(scriptFile, "loadManually")) {
                        return new ScriptLoadResult(false, "Script file will only load manually");
                    }
                }
            }

            unloadScript(scriptId);

            Future<?> future = executorService.submit(() -> {
                V8Runtime localScriptEngine = null;
                V8ValueObject globalObject = null;
                try {
                    // IMPORTANT: V8Runtime must be created and used on the same thread
                    localScriptEngine = coolcostupit.openjs.modules.ScriptEngine.getEngine();
                    final V8Runtime finalEngine = localScriptEngine; // Final reference for use in inner blocks
                    scriptEngines.put(scriptId, localScriptEngine);

                    // Set global bindings directly (JavetJVMInterceptor handles Java-to-JS conversion)
                    globalObject = localScriptEngine.getGlobalObject();

                    // Bind Java objects directly to global scope
                    globalObject.set("plugin", plugin);
                    globalObject.set("scriptManager", this);
                    globalObject.set("scriptEngine", finalEngine);
                    globalObject.set("log", new ScriptLogger(getLogger(), scriptId));
                    globalObject.set("variableStorage", variableStorage);
                    globalObject.set("DiskStorage", sharedClass.DiskStorageApi);
                    globalObject.set("publicVarManager", PublicVarManager);
                    globalObject.set("_task", taskApi);
                    globalObject.set("_libImporter", sharedClass.LibImporterApi);
                    globalObject.set("_internalPluginLogger", Logger);
                    globalObject.set("currentScriptName", scriptId);
                    globalObject.set("IsFoliaServer", FoliaSupport.isFolia());

                    // Manually expose Java class loader for dynamic class loading
                    globalObject.set("__classLoader", Thread.currentThread().getContextClassLoader());

                    // V8 Protection: Make critical properties read-only
                    // Note: Java objects (plugin, scriptManager, etc.) are already immutable from JavaScript
                    // V8's proxy system prevents us from freezing Java objects like we could in GraalJS
                    finalEngine.getExecutor("""
                    // Create Java package access system using Proxy
                    function createPackageProxy(packageName) {
                        const cache = {};
                        return new Proxy({}, {
                            get(target, prop) {
                                if (prop === Symbol.toStringTag) return 'JavaPackage';
                                if (typeof prop === 'symbol') return undefined;

                                const fullName = packageName ? packageName + '.' + prop : prop;

                                // Check cache first
                                if (cache[prop]) return cache[prop];

                                try {
                                    // Try to load as a class
                                    const javaClass = __classLoader.loadClass(fullName);
                                    cache[prop] = javaClass;
                                    return javaClass;
                                } catch (e) {
                                    // Not a class, treat as a package
                                    const subPackage = createPackageProxy(fullName);
                                    cache[prop] = subPackage;
                                    return subPackage;
                                }
                            },
                            has(target, prop) {
                                return true; // Allow any property access
                            }
                        });
                    }

                    // Create root package proxies
                    globalThis.org = createPackageProxy('org');
                    globalThis.java = createPackageProxy('java');
                    globalThis.com = createPackageProxy('com');
                    globalThis.net = createPackageProxy('net');
                    globalThis.io = createPackageProxy('io');

                    // Make currentScriptName and IsFoliaServer immutable
                    Object.defineProperty(this, 'currentScriptName', {
                      value: currentScriptName,
                      writable: false,
                      configurable: false,
                      enumerable: true
                    });

                    Object.defineProperty(this, 'IsFoliaServer', {
                      value: IsFoliaServer,
                      writable: false,
                      configurable: false,
                      enumerable: true
                    });
                    """).executeVoid();

                    if (configUtil.getConfigFromBuffer("AllowFeatureFlags", true)) {
                        if (FlagInterpreter.hasFlag(scriptFile, "waitForInit")) {
                            finalEngine.getExecutor("scriptManager.waitForInit()").executeVoid();
                        }
                    }

                    finalEngine.getExecutor(JavascriptHelper.JAVASCRIPT_CODE).executeVoid();
                    List BridgesToLoad = FlagInterpreter.getFlags(scriptFile);

                    if (!BridgesToLoad.isEmpty()) {
                        BridgeLoader.loadBridges(BridgesToLoad, scriptId, finalEngine);
                    }

                    String processedScript = preprocessScript(scriptFile, finalEngine, globalObject);

                    // Debug: Check what _internalPluginLogger actually is
                    try {
                        String debugInfo = finalEngine.getExecutor("""
                            (function() {
                                let info = 'Type: ' + typeof _internalPluginLogger + ', ';
                                info += 'HasMethod: ' + (typeof _internalPluginLogger?.internalException) + ', ';
                                info += 'Keys: ' + Object.keys(_internalPluginLogger || {}).join(',');
                                return info;
                            })()
                        """).executeString();
                        Logger.scriptlog(Level.INFO, scriptId, "Debug - _internalPluginLogger: " + debugInfo, pluginLogger.CYAN);
                    } catch (Exception e) {
                        Logger.scriptlog(Level.SEVERE, scriptId, "Debug check failed: " + e.getMessage(), pluginLogger.RED);
                    }

                    finalEngine.getExecutor(processedScript).executeVoid();
                    if (configUtil.getConfigFromBuffer("PrintScriptActivations", true)) {
                        Logger.log(Level.INFO, "Loaded the script " + scriptId, pluginLogger.GREEN);
                    }
                    FoliaSupport.runTaskSynchronously(plugin, () -> //plugin.getServer().getScheduler().runTask(plugin, () ->
                            plugin.getServer().getPluginManager().callEvent(new ScriptLoadedEvent(scriptId)));
                } catch (IOException | JavetException e) {
                    Logger.scriptlog(Level.WARNING,  scriptId, "Failed to load script " + e.getMessage(), pluginLogger.ORANGE);
                } finally {
                    // Don't close globalObject - V8Value references need to stay alive
                    // for commands, events, and other callbacks. The V8Runtime will handle cleanup.
                    // Note: globalObject will be cleaned up when the V8Runtime is closed during unloadScript()
                }
            });

            if (!activeFiles.contains(scriptId)) {
                activeFiles.add(scriptId);
            }

            if (!runningScripts.contains(scriptId)) {
                runningScripts.add(scriptId);
            }

            scriptFutures.put(scriptId, future);
            return new ScriptLoadResult(true, "Script loaded successfully.");
        }
        return new ScriptLoadResult(false, "Invalid script file.");
    }

    public void loadScripts() {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        unloadAllScripts(); // simple fix, unload all scripts before loading them again, this is why I love programming
        if (scriptsFolder.exists() && scriptsFolder.isDirectory()) {
            List<Future<?>> futures = new ArrayList<>();

            // Use recursive scan to find all scripts including those in subfolders
            List<File> allScripts = getAllScriptFiles(scriptsFolder);
            for (File scriptFile : allScripts) {
                Future<?> future = executorService.submit(() -> loadScript(scriptFile, false));
                futures.add(future);
            }

            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    Logger.log(Level.WARNING, "An error occurred while waiting for script loading tasks to complete: " + e.getMessage(), pluginLogger.ORANGE);
                }
            }
        }
    }

    // In-Build script functions: (HELPERS)
    public void registerCommand(String commandName, V8ValueObject commandHandler, String scriptName, V8Runtime scriptEngine, @Nullable String permission) {
        try {
            CommandMap commandMap = getCommandMap();

            // Store the handler in the V8 global scope for later access
            String handlerId = "__cmd_" + commandName.replace("-", "_");
            try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                globalObject.set(handlerId, commandHandler);
            }

            Command dynamicCommand = new Command(commandName) {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
                    if (!testPermission(sender)) return true; // permission check, may be redundant
                    try {
                        // V8 is single-threaded - set arguments in global scope and execute
                        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                            globalObject.set("__cmd_sender", sender);
                            globalObject.set("__cmd_args", args);
                        }

                        scriptEngine.getExecutor(String.format("""
                            (function() {
                                const handler = globalThis.%s;
                                if (handler && typeof handler.onCommand === 'function') {
                                    handler.onCommand(globalThis.__cmd_sender, globalThis.__cmd_args);
                                }
                                delete globalThis.__cmd_sender;
                                delete globalThis.__cmd_args;
                            })();
                            """, handlerId
                        )).executeVoid();
                    } catch (Exception e) {
                        sender.sendMessage(chatColors.RED + "An error occurred while executing the command: " + e.getMessage());
                        Logger.scriptlog(Level.SEVERE, scriptName, "Error in script command execution for " + commandName + ": " + e.getMessage(), pluginLogger.ORANGE);
                    }
                    return true;
                }

                @Override
                public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
                    try {
                        // V8 is single-threaded - set arguments in global scope and execute
                        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                            globalObject.set("__cmd_sender", sender);
                            globalObject.set("__cmd_args", args);
                        }

                        Object result = scriptEngine.getExecutor(String.format("""
                            (function() {
                                const handler = globalThis.%s;
                                if (handler && typeof handler.onTabComplete === 'function') {
                                    const res = handler.onTabComplete(globalThis.__cmd_sender, globalThis.__cmd_args);
                                    delete globalThis.__cmd_sender;
                                    delete globalThis.__cmd_args;
                                    return res;
                                }
                                delete globalThis.__cmd_sender;
                                delete globalThis.__cmd_args;
                                return null;
                            })();
                            """, handlerId
                        )).executeObject();

                        if (result instanceof List) {
                            return (List<String>) result;
                        }
                    } catch (Exception e) {
                        Logger.scriptlog(Level.WARNING, scriptName, "Error during tab-completion for command " + commandName + ": " + e.getMessage(), pluginLogger.ORANGE);
                    }
                    return super.tabComplete(sender, alias, args);
                }
            };

            if (permission != null && !permission.isEmpty()) {
                dynamicCommand.setPermission(permission);
            }

            commandMap.register(plugin.getName(), dynamicCommand);
            scriptCommands.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(dynamicCommand);
            invokeSyncCommands(); // Update command map for tab completion

            if (configUtil.getConfigFromBuffer("LogCustomCommandsActivity", true)) {
                Logger.log(Level.INFO, "[" + scriptName + "] Registered command: " + commandName, pluginLogger.GREEN);
            }
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register command " + commandName + ": " + e.getMessage(), pluginLogger.RED);
        }
    }

    public void waitForInit() {
        while (!scriptsReady) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // TODO: Remove in 1.1.3 (In favor of taskApi)
    public void registerSchedule(String scriptName, long delay, long period, V8ValueObject handler, V8Runtime scriptEngine, String methodName) {
        Logger.scriptlog(Level.WARNING, scriptName, "Do not use registerSchedule! This will get removed soon, use task.repeat instead!", pluginLogger.ORANGE);

        // Store handler for cross-thread access
        String handlerId = "__schedule_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.log(Level.SEVERE, "["+scriptName+"] Failed to register schedule: " + e.getMessage(), pluginLogger.RED);
            return;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.%s === 'function') {
                            handler.%s();
                        }
                    })();
                    """, handlerId, methodName, methodName
                )).executeVoid();
            } catch (Exception e) {
                Logger.log(Level.SEVERE, "["+scriptName+"] Schedule execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };

        int taskId;
        if (period > 0) {
            taskId = FoliaSupport.ScheduleRepeatingTask(plugin, task, delay, period);
        } else {
            taskId = FoliaSupport.ScheduleTask(plugin, task, delay);
        }

        scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));
    }

    public Listener registerEvent(String eventClassName, V8ValueObject handler, String scriptName, V8Runtime scriptEngine) throws JavetException {
        try {
            Class<?> eventClass = Class.forName(eventClassName);
            if (Event.class.isAssignableFrom(eventClass)) {
                Class<? extends Event> eventClassCasted = (Class<? extends Event>) eventClass;

                // Store handler for the inline executor too
                String handlerId = "__event_inline_" + System.nanoTime();
                try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                    globalObject.set(handlerId, handler);
                }

                Listener listener = new EventListenerWrapper(scriptEngine, handler, plugin);
                getServer().getPluginManager().registerEvent(eventClassCasted, listener, EventPriority.NORMAL, (l, e) -> {
                    try {
                        // Set event in global scope and execute
                        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                            globalObject.set("__event_data", e);
                        }

                        scriptEngine.getExecutor(String.format("""
                            (function() {
                                const handler = globalThis.%s;
                                if (handler && typeof handler.handleEvent === 'function') {
                                    handler.handleEvent(globalThis.__event_data);
                                }
                                delete globalThis.__event_data;
                            })();
                            """, handlerId
                        )).executeVoid();
                    } catch (Exception ex) {
                        Logger.log(Level.SEVERE, "[" + scriptName + "] Event handler error: " + ex.getMessage(), pluginLogger.RED);
                    }
                }, plugin);

                eventListenersMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(listener);
                return listener;
            } else {
                Logger.scriptlog(Level.WARNING, scriptName, "Class " + eventClassName + " is not an Event.", pluginLogger.ORANGE);
            }
        } catch (ClassNotFoundException e) {
            Logger.scriptlog(Level.WARNING, scriptName, "Failed to register event " + eventClassName + ": " + e.getMessage(), pluginLogger.ORANGE);
        }
        return null;
    }

}
