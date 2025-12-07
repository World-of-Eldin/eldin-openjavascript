/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

public class ScriptEngine {
    private static Engine graalEngine;

    /**
     * Get a new GraalJS ScriptEngine instance with Nashorn compatibility mode enabled.
     * Each call returns a fresh engine instance for script isolation.
     *
     * @return A new javax.script.ScriptEngine instance backed by GraalJS
     */
    public static javax.script.ScriptEngine getEngine() {
        if (graalEngine == null) {
            // Create a shared GraalVM engine for better performance
            // The engine can be shared across contexts while each ScriptEngine gets its own Context
            graalEngine = Engine.newBuilder()
                    .allowExperimentalOptions(true)
                    .option("engine.WarnInterpreterOnly", "false")
                    .build();
        }

        // Create a new ScriptEngine with Nashorn compatibility mode
        // This ensures existing scripts using Java.type(), Java.to(), etc. continue to work
        return GraalJSScriptEngine.create(graalEngine,
                Context.newBuilder("js")
                        .allowExperimentalOptions(true)
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(className -> true)
                        .option("js.nashorn-compat", "true")
                        .option("js.ecmascript-version", "2022"));
    }
}
