/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.pluginbridges;

import com.comphenix.protocol.PacketType;
import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;

import javax.script.ScriptEngine;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class BridgeLoader {

    private static final String BRIDGE_PACKAGE = "coolcostupit.openjs.BridgeLoaders";
    private static final pluginLogger Logger = sharedClass.logger;

    public static void loadBridges(List<String> bridgesToLoad, String scriptName, ScriptEngine engine) {
        for (String bridgeName : bridgesToLoad) {
            try {
                // Full class name (package + class name)
                String className = BRIDGE_PACKAGE + "." + bridgeName;
                Class<?> clazz = Class.forName(className);
                Object bridgeInstance = clazz.getDeclaredConstructor().newInstance(); // Create an instance (must have public no-arg constructor)
                Method loadMethod = clazz.getMethod("Load", String.class, ScriptEngine.class);
                loadMethod.invoke(bridgeInstance, scriptName, engine);
                //Logger.scriptlog(Level.INFO, scriptName, "[BridgeLoader] Loaded bridge: " + bridgeName, pluginLogger.LIGHT_BLUE);
            } catch (ClassNotFoundException e) {
                //("[BridgeLoader] Bridge class not found: " + bridgeName); Ignore
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "[BridgeLoader] Error loading bridge: " + bridgeName, pluginLogger.ORANGE);
                Logger.scriptlog(Level.WARNING, scriptName, "[BridgeLoader] " + e.getMessage(), pluginLogger.ORANGE);
            }
        }
    }

    public static PacketType resolvePacketType(String path) throws Exception {
        String[] parts = path.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid PacketType format: " + path);
        }

        String protocol = parts[0];
        String side = parts[1];
        String name = parts[2];

        Class<?> holderClass = Class.forName("com.comphenix.protocol.PacketType$" + protocol + "$" + side);
        return (PacketType) holderClass.getField(name).get(null);
    }

    public static PacketType[] resolvePacketTypes(String scriptName, List<String> paths) {
        List<PacketType> result = new ArrayList<>();
        for (String path : paths) {
            try {
                result.add(resolvePacketType(path));
            } catch (Exception e) {
                Logger.scriptlog(Level.WARNING, scriptName, "Invalid packet type: " + path, pluginLogger.RED);
            }
        }
        return result.toArray(new PacketType[0]);
    }

}
