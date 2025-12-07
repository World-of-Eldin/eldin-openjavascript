/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import coolcostupit.openjs.logging.pluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class scriptTaskerApi {
    private final scriptWrapper ScriptWrapper;
    private final @NotNull PluginManager pluginManager;
    private final pluginLogger Logger;
    private static final Map<Object, ListenerEntry> listenerCleanupMap = new HashMap<>();

    private static class ListenerEntry {
        public final String scriptName;
        public final Object cleanup;
        public final ScriptEngine scriptEngine;

        public ListenerEntry(String scriptName, ScriptEngine scriptEngine, Object cleanup) {
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

    public Boolean wait(String scriptName, ScriptEngine scriptEngine, Number seconds) {
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

    public int spawn(String scriptName, ScriptEngine scriptEngine, Object handler) {
        Runnable task = () -> {
            try {
                ((Invocable) scriptEngine).invokeMethod(handler, "f");
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.scriptlog(Level.WARNING, scriptName, e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runTask(sharedClass.plugin, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int entitySchedule(String scriptName, ScriptEngine scriptEngine, Entity entity, Object handler) {
        Runnable task = () -> {
            try {
                ((Invocable) scriptEngine).invokeMethod(handler, "f");
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.scriptlog(Level.WARNING, scriptName, e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runEntityTask(sharedClass.plugin, entity, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int main(String scriptName, ScriptEngine scriptEngine, Object handler) {
        Runnable task = () -> {
            try {
                ((Invocable) scriptEngine).invokeMethod(handler, "f");
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.scriptlog(Level.WARNING, scriptName, e.getMessage(), pluginLogger.RED);
            }
        };
        int taskId = FoliaSupport.runTaskSynchronously(sharedClass.plugin, task);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int delay(String scriptName, ScriptEngine scriptEngine, Number Delay, Object handler) {
        double sec = Delay.doubleValue();

        if (sec <= 0) return 0;
        long ticks = (long) (sec * 20); // Convert seconds to ticks

        Runnable task = () -> {
            try {
                ((Invocable) scriptEngine).invokeMethod(handler, "f");
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.scriptlog(Level.WARNING, scriptName, e.getMessage(), pluginLogger.RED);
            }
        };

        int taskId = FoliaSupport.DelayTask(sharedClass.plugin, task, ticks);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(Integer.valueOf(taskId));

        return taskId;
    }

    public int repeat(String scriptName, ScriptEngine scriptEngine, Number Delay, Number Period, Object handler) {
        double delaySec = Delay.doubleValue();
        double periodSec = Period.doubleValue();

        if (delaySec < 0) {
            Logger.log(Level.WARNING, "[" + scriptName + "] Invalid repeat delay/period values: delay=" + delaySec + ", period=" + periodSec, pluginLogger.RED);
            return 0;
        }

        long delayTicks = (long) (delaySec * 20);   // Delay before first run
        long periodTicks = (long) (periodSec * 20); // Interval between runs

        Runnable task = () -> {
            try {
                ((Invocable) scriptEngine).invokeMethod(handler, "f");
            } catch (ScriptException | NoSuchMethodException e) {
                Logger.scriptlog(Level.WARNING, scriptName, e.getMessage(), pluginLogger.RED);
            }
        };

        int taskId = FoliaSupport.ScheduleRepeatingTask(sharedClass.plugin, task, delayTicks, periodTicks);
        ScriptWrapper.scriptTasksMap.computeIfAbsent(scriptName, k -> new ArrayList<>()).add(taskId);

        return taskId;
    }

    public void cleanupListener(String scriptName, ScriptEngine scriptEngine, Object handler) {
        try {
            Logger.log(Level.INFO, "[" + scriptName + "] Listener cleanup executed.", pluginLogger.LIGHT_BLUE);
            ((Invocable) scriptEngine).invokeMethod(handler, "f");
        } catch (ScriptException | NoSuchMethodException e) {
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

    public <T> Object createListener(String scriptName, ScriptEngine engine, Class<T> interfaceClass, Object jsHandler) {
        if (!(engine instanceof Invocable)) {
            Logger.log(Level.WARNING, "Script engine is not invocable. Cannot bind handler.", pluginLogger.RED);
            return null;
        }

        Invocable inv = (Invocable) engine;

        Object proxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                (p, method, args) -> {
                    try {
                        return inv.invokeMethod(jsHandler, method.getName(), args);
                    } catch (NoSuchMethodException e) {
                        if (method.getReturnType().isPrimitive()) {
                            if (method.getReturnType() == boolean.class) return false;
                            if (method.getReturnType() == char.class) return '\0';
                            return 0;
                        }
                        return null;
                    } catch (Exception e) {
                        Logger.log(Level.SEVERE, "Listener handler error: " + e.getMessage(), pluginLogger.RED);
                        return null;
                    }
                }
        );

        return proxy;
    }

    public void setListenerCleanup(String scriptName, ScriptEngine scriptEngine, Object proxy, Object cleanup) {
        if (cleanup != null) {
            listenerCleanupMap.put(proxy, new ListenerEntry(scriptName, scriptEngine, cleanup));
        } else {
            Logger.log(Level.WARNING, "[" + scriptName + "] Listener created without cleanup function. This may cause memory leaks.", pluginLogger.ORANGE);
        }
    }
}
