/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ScriptUnloadedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String scriptName;

    public ScriptUnloadedEvent(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScriptName() {
        return scriptName;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
