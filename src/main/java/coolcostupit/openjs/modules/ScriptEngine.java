/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import com.caoccao.javet.exceptions.JavetException;
import com.caoccao.javet.interception.jvm.JavetJVMInterceptor;
import com.caoccao.javet.interop.V8Host;
import com.caoccao.javet.interop.V8Runtime;
import com.caoccao.javet.interop.converters.JavetProxyConverter;

public class ScriptEngine {

    /**
     * Get a new Javet V8Runtime instance with V8 backend.
     * Each call returns a fresh runtime instance for script isolation.
     * Supports modern ECMAScript 2024+ features including:
     * - async/await
     * - optional chaining (?.)
     * - BigInt
     * - const/let/arrow functions
     * - classes and modules
     * - and all other ES2024 features
     *
     * @return A new V8Runtime instance with V8 backend
     * @throws JavetException if the runtime cannot be created
     */
    public static V8Runtime getEngine() throws JavetException {
        // Create V8Runtime directly (not using engine pool for better interceptor compatibility)
        V8Runtime runtime = V8Host.getV8Instance().createV8Runtime();

        // Set up proxy converter for seamless Java-JS object conversion
        JavetProxyConverter proxyConverter = new JavetProxyConverter();
        runtime.setConverter(proxyConverter);

        // Create and register the JVM interceptor for Java class access
        // IMPORTANT: We must keep a reference to prevent garbage collection
        JavetJVMInterceptor javetJVMInterceptor = new JavetJVMInterceptor(runtime);
        try (com.caoccao.javet.values.reference.V8ValueObject globalObject = runtime.getGlobalObject()) {
            javetJVMInterceptor.register(globalObject);

            // Store the interceptor as a private property to prevent GC
            // This is crucial - without this, the interceptor gets garbage collected
            globalObject.setPrivateProperty("__jvmInterceptor", javetJVMInterceptor);
        }

        return runtime;
    }
}
