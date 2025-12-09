/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.BridgeLoaders;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;
import coolcostupit.openjs.pluginbridges.ProtocolLibBridge;

import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.values.reference.V8ValueObject;
import java.util.logging.Level;

public class ProtocolLib {
    public void Load(String ScriptName, V8Runtime Engine) {
        try {
            V8ValueObject globalObject = Engine.getGlobalObject();
            globalObject.set("ProtocolLib", new ProtocolLibBridge(Engine, ScriptName));
        } catch (Exception e) {
            sharedClass.logger.scriptlog(Level.WARNING, ScriptName, "Failed to load script " + e.getMessage(), pluginLogger.ORANGE);
        }
    }
}
