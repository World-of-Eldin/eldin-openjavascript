/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.pluginbridges;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.scriptWrapper;
import coolcostupit.openjs.modules.sharedClass;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static coolcostupit.openjs.pluginbridges.BridgeLoader.resolvePacketTypes;

public class ProtocolLibBridge {
    private final ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    private final Map<Object, PacketListener> scriptListeners = new ConcurrentHashMap<>();
    private final Map<String, List<PacketListener>> TotalScriptListeners = new ConcurrentHashMap<>();

    public final ScriptEngine engine;
    public final String scriptName;
    public final pluginLogger Logger;

    public ProtocolLibBridge(ScriptEngine engine, String scriptName) {
        this.engine = engine;
        this.scriptName = scriptName;
        this.Logger = sharedClass.logger;
        scriptWrapper.addToCleanupMap(scriptName, this::clearListeners);
    }

    public PacketListener registerListener(String Priority, Object jsHandler, List<String> packetTypeStrings) {
        PacketType[] types = resolvePacketTypes(scriptName, packetTypeStrings);
        PacketAdapter adapter = new PacketAdapter(sharedClass.plugin, ListenerPriority.valueOf(Priority), types) {
            @Override
            public void onPacketSending(PacketEvent event) {
                invokeJS(jsHandler, "onSend", event);
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {
                invokeJS(jsHandler, "onReceive", event);
            }
        };

        manager.addPacketListener(adapter);
        scriptListeners.put(adapter, adapter);
        TotalScriptListeners.computeIfAbsent(this.scriptName, k -> new ArrayList<>()).add(adapter);

        return adapter;
    }

    public void unregisterListener(Object adapter) {
        PacketListener listener = scriptListeners.remove(adapter);
        if (listener != null) {
            manager.removePacketListener(listener);
        }
    }

    public void clearListeners() {
        List<PacketListener> listeners = TotalScriptListeners.remove(scriptName);
        if (listeners != null) {
            for (PacketListener listener : listeners) {
                manager.removePacketListener(listener);
                //Logger.scriptlog(Level.INFO, scriptName, "[ProtocolLib] Listener destroyed", pluginLogger.LIGHT_BLUE);
            }
        }
    }

    private void invokeJS(Object handler, String method, PacketEvent event) {
        try {
            ((Invocable) engine).invokeMethod(handler, method, event);
        } catch (Exception e) {
            Logger.scriptlog(Level.WARNING, scriptName, "[ProtocolLib] " + method + " failed: " + e.getMessage(), pluginLogger.ORANGE);
        }
    }
}