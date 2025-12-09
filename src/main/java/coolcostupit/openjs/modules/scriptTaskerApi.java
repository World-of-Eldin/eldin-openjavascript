/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;

import coolcostupit.openjs.logging.pluginLogger;

public class scriptTaskerApi {
    private final scriptWrapper ScriptWrapper;
    private final @NotNull PluginManager pluginManager;
    private final pluginLogger Logger;
    private static final Map<Object, ListenerEntry> listenerCleanupMap = new HashMap<>();

    private static class ListenerEntry {
        public final String scriptName;
        public final V8ValueObject cleanup;
        public final V8Runtime scriptEngine;

        public ListenerEntry(String scriptName, V8Runtime scriptEngine, V8ValueObject cleanup) {
            this.scriptName = scriptName;
            this.cleanup = cleanup;
            this.scriptEngine = scriptEngine;
        }
    }

    public scriptTaskerApi(scriptWrapper scriptWrapper) {
        this.ScriptWrapper = scriptWrapper;
        this.pluginManager = Bukkit.getPluginManager();
        this.Logger = sharedClass.logger;
    }

    public Boolean wait(String scriptName, V8Runtime scriptEngine, Number seconds) {
        double sec = seconds.doubleValue();

        if (sec <= 0) return Boolean.TRUE;

        if (Bukkit.isPrimaryThread()) {
            Logger.log(
                    Level.WARNING,
                    "[" + scriptName + "] Calling task.wait(" + sec + "s) on the main server thread can cause lag or freeze the server!",
                    pluginLogger.ORANGE
            );
        }

        long millis = (long) (sec * 1000);
        if (millis < 0) millis = 0;

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.log(Level.INFO, "[" + scriptName + "] interrupting task.wait(" + seconds + ")", pluginLogger.LIGHT_BLUE);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    public void waitForScript(String scriptName) {
        while (!ScriptWrapper.isJavascriptFileRunning(scriptName)) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void waitForPlugin(String pluginName, String scriptName) {
        Plugin plugin = pluginManager.getPlugin(pluginName);

        if (plugin == null) {
            Logger.log(Level.WARNING, "[" + scriptName + "] Plugin \"" + pluginName + "\" does not exist.", pluginLogger.ORANGE);
            return;
        }

        if (plugin.isEnabled()) {
            return; // Already loaded
        }

        while (!plugin.isEnabled()) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public int spawn(String scriptName, V8Runtime scriptEngine, V8ValueObject handler) {
        // Store function wrapper in global scope for cross-thread access
        String handlerId = "__task_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register task: " + e.getMessage(), pluginLogger.RED);
            return 0;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.f === 'function') {
                            handler.f();
                        }
                    })();
                    """, handlerId
                )).executeVoid();
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Task execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runTask(sharedClass.plugin, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int entitySchedule(String scriptName, V8Runtime scriptEngine, Entity entity, V8ValueObject handler) {
        String handlerId = "__task_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register entity task: " + e.getMessage(), pluginLogger.RED);
            return 0;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.f === 'function') {
                            handler.f();
                        }
                    })();
                    """, handlerId
                )).executeVoid();
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Entity task execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runEntityTask(sharedClass.plugin, entity, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int main(String scriptName, V8Runtime scriptEngine, V8ValueObject handler) {
        String handlerId = "__task_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register main task: " + e.getMessage(), pluginLogger.RED);
            return 0;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.f === 'function') {
                            handler.f();
                        }
                    })();
                    """, handlerId
                )).executeVoid();
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Main task execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runTaskSynchronously(sharedClass.plugin, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int delay(String scriptName, V8Runtime scriptEngine, Number Delay, V8ValueObject handler) {
        double sec = Delay.doubleValue();

        if (sec <= 0) return 0;
        long ticks = (long) (sec * 20); // Convert seconds to ticks

        String handlerId = "__task_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register delayed task: " + e.getMessage(), pluginLogger.RED);
            return 0;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.f === 'function') {
                            handler.f();
                        }
                        delete globalThis.%s;
                    })();
                    """, handlerId, handlerId
                )).executeVoid();
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Delayed task execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };

        int taskId = FoliaSupport.DelayTask(sharedClass.plugin, task, ticks);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int repeat(String scriptName, V8Runtime scriptEngine, Number Delay, Number Period, V8ValueObject handler) {
        double delaySec = Delay.doubleValue();
        double periodSec = Period.doubleValue();

        if (delaySec < 0) {
            Logger.log(Level.WARNING, "[" + scriptName + "] Invalid repeat delay/period values: delay=" + delaySec + ", period=" + periodSec, pluginLogger.RED);
            return 0;
        }

        long delayTicks = (long) (delaySec * 20);   // Delay before first run
        long periodTicks = (long) (periodSec * 20); // Interval between runs

        String handlerId = "__task_" + System.nanoTime();
        try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            Logger.scriptlog(Level.SEVERE, scriptName, "Failed to register repeating task: " + e.getMessage(), pluginLogger.RED);
            return 0;
        }

        Runnable task = () -> {
            try {
                scriptEngine.getExecutor(String.format("""
                    (function() {
                        const handler = globalThis.%s;
                        if (handler && typeof handler.f === 'function') {
                            handler.f();
                        }
                    })();
                    """, handlerId
                )).executeVoid();
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Repeating task execution error: " + e.getMessage(), pluginLogger.RED);
            }
        };

        int taskId = FoliaSupport.ScheduleRepeatingTask(sharedClass.plugin, task, delayTicks, periodTicks);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(taskId);

        return taskId;
    }

    public void cleanupListener(String scriptName, V8Runtime scriptEngine, V8ValueObject handler) {
        try {
            Logger.log(Level.INFO, "[" + scriptName + "] Listener cleanup executed.", pluginLogger.LIGHT_BLUE);

            String handlerId = "__cleanup_" + System.nanoTime();
            try (V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
                globalObject.set(handlerId, handler);
            }

            scriptEngine.getExecutor(String.format("""
                (function() {
                    const handler = globalThis.%s;
                    if (handler && typeof handler.f === 'function') {
                        handler.f();
                    }
                    delete globalThis.%s;
                })();
                """, handlerId, handlerId
            )).executeVoid();
        } catch (Exception e) {
            Logger.scriptlog(Level.WARNING, scriptName, "Listener cleanup failed: " + e.getMessage(), pluginLogger.RED);
        }
    }

    public void cancel(String scriptName, Object thing) {
        if (thing instanceof Integer) {
            int taskId = (int) thing;
            List<Integer> taskIds = ScriptWrapper.scriptTasksMap.get(scriptName);

            if (taskIds != null && taskIds.remove(Integer.valueOf(taskId))) {
                FoliaSupport.CancelTask(taskId);
                Logger.log(Level.INFO, "[" + scriptName + "] Unregistered task ID " + taskId, pluginLogger.LIGHT_BLUE);

                if (taskIds.isEmpty()) {
                    ScriptWrapper.scriptTasksMap.remove(scriptName);
                }
            } else {
                Logger.log(Level.WARNING, "[" + scriptName + "] Tried to unregister unknown task ID " + taskId, pluginLogger.ORANGE);
            }
            return;
        }
        ListenerEntry entry = listenerCleanupMap.remove(thing);
        if (entry != null) {
            cleanupListener(entry.scriptName, entry.scriptEngine, entry.cleanup);
        } else {
            Logger.log(Level.WARNING, "[" + scriptName + "] Tried to cancel unknown listener or missing cleanup.", pluginLogger.ORANGE);
        }
    }

    public void clearListeners(String scriptName) {
        List<Object> toRemove = new ArrayList<>();

        for (Map.Entry<Object, ListenerEntry> entryValue : listenerCleanupMap.entrySet()) {
            ListenerEntry entry = entryValue.getValue();
            if (entry.scriptName.equals(scriptName)) {
                cleanupListener(entry.scriptName, entry.scriptEngine, entry.cleanup);
                toRemove.add(entryValue.getKey());
            }
        }

        toRemove.forEach(listenerCleanupMap::remove);
    }

    public <T> Object createListener(String scriptName, V8Runtime engine, Class<T> interfaceClass, V8ValueObject jsHandler) {
        try {
            Object proxy = Proxy.newProxyInstance(
                    interfaceClass.getClassLoader(),
                    new Class<?>[]{interfaceClass},
                    (p, method, args) -> {
                        try {
                            // Try to get the method from the JavaScript object
                            try (com.caoccao.javet.values.reference.V8ValueFunction fn = jsHandler.get(method.getName())) {
                                if (fn != null && !fn.isUndefined()) {
                                    return fn.callObject(jsHandler, args);
                                }
                            }
                            // Method not found - return default value
                            if (method.getReturnType().isPrimitive()) {
                                if (method.getReturnType() == boolean.class) return false;
                                if (method.getReturnType() == char.class) return '\0';
                                return 0;
                            }
                            return null;
                        } catch (Exception e) {
                            Logger.log(Level.SEVERE, "Listener handler error: " + e.getMessage(), pluginLogger.RED);
                            if (method.getReturnType().isPrimitive()) {
                                if (method.getReturnType() == boolean.class) return false;
                                if (method.getReturnType() == char.class) return '\0';
                                return 0;
                            }
                            return null;
                        }
                    }
            );

            return proxy;
        } catch (Exception e) {
            Logger.log(Level.SEVERE, "Failed to create listener proxy: " + e.getMessage(), pluginLogger.RED);
            return null;
        }
    }

    public void setListenerCleanup(String scriptName, V8Runtime scriptEngine, Object proxy, V8ValueObject cleanup) {
        if (cleanup != null) {
            listenerCleanupMap.put(proxy, new ListenerEntry(scriptName, scriptEngine, cleanup));
        } else {
            Logger.log(Level.WARNING, "[" + scriptName + "] Listener created without cleanup function. This may cause memory leaks.", pluginLogger.ORANGE);
        }
    }
}
