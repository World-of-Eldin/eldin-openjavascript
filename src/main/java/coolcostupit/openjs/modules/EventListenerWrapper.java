/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventListenerWrapper implements Listener, EventExecutor {
    private final Invocable invocable;
    private final Object handler;
    private final Logger logger;

    public EventListenerWrapper(ScriptEngine scriptEngine, Object handler, Plugin plugin) {
        this.invocable = (Invocable) scriptEngine;
        this.handler = handler;
        this.logger = plugin.getLogger();
    }

    @Override
    public void execute(Listener listener, Event event) {
        try {
            invocable.invokeMethod(handler, "handle", event);
        } catch (ScriptException | NoSuchMethodException e) {
            logger.log(Level.WARNING, "Failed to execute script event-handler: ", e);
        }
    }
}
