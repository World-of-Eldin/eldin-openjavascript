/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.V8ValueFunction;
import com.caoccao.javet.values.reference.V8ValueObject;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EventListenerWrapper implements Listener, EventExecutor {
    private final V8Runtime runtime;
    private final String handlerId;
    private final Logger logger;

    public EventListenerWrapper(V8Runtime scriptEngine, V8ValueObject handler, Plugin plugin) {
        this.runtime = scriptEngine;
        this.logger = plugin.getLogger();

        // Store handler in V8 global scope for cross-thread access
        this.handlerId = "__event_" + System.nanoTime();
        try (com.caoccao.javet.values.reference.V8ValueObject globalObject = scriptEngine.getGlobalObject()) {
            globalObject.set(handlerId, handler);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to register event handler: " + e.getMessage());
            throw new RuntimeException("Failed to register event handler", e);
        }
    }

    @Override
    public void execute(Listener listener, Event event) {
        try {
            // Set event in global scope and execute
            try (com.caoccao.javet.values.reference.V8ValueObject globalObject = runtime.getGlobalObject()) {
                globalObject.set("__event_data", event);
            }

            runtime.getExecutor(String.format("""
                (function() {
                    const handler = globalThis.%s;
                    if (handler && typeof handler.handle === 'function') {
                        handler.handle(globalThis.__event_data);
                    }
                    delete globalThis.__event_data;
                })();
                """, handlerId
            )).executeVoid();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to execute script event-handler: " + e.getMessage());
        }
    }
}
