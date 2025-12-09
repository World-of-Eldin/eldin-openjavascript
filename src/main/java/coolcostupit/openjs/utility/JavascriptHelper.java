/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.utility;

import coolcostupit.openjs.logging.pluginLogger;
import coolcostupit.openjs.modules.sharedClass;

public class JavascriptHelper {
    private static final String MAIN_JAVASCRIPT_CODE = String.format("""
                var _gc = false;
                
                function _unloadThis() {
                  const keys = Object.keys(this);
                  for (let i = 0; i < keys.length; i++) {
                    this[keys[i]] = null;
                  }
                  _gc = true;
                };
                
                Object.defineProperty(this, "_unloadThis", {
                    writable: false,
                    configurable: false
                });
                
                const toArray = args => Array.prototype.slice.call(args);
                // Javet handles Java/JavaScript type conversion automatically
                const toJavaList = data => data;

                // Java utility object for compatibility with GraalJS scripts
                const Java = {
                    // Convert Java collections/arrays to JavaScript arrays
                    from: function(javaCollection) {
                        if (javaCollection === null || javaCollection === undefined) {
                            return [];
                        }
                        // Javet automatically converts Java arrays and collections to JS arrays
                        // But we provide this for compatibility
                        if (Array.isArray(javaCollection)) {
                            return javaCollection;
                        }
                        // Try to convert iterable
                        try {
                            return Array.from(javaCollection);
                        } catch (e) {
                            // Fallback: try to iterate manually
                            const result = [];
                            const iter = javaCollection.iterator();
                            while (iter.hasNext()) {
                                result.push(iter.next());
                            }
                            return result;
                        }
                    },
                    // Load a Java class by name (alternative to package notation)
                    type: function(className) {
                        try {
                            return java.lang.Class.forName(className);
                        } catch (e) {
                            throw new Error('Class not found: ' + className);
                        }
                    }
                };

                const addCommand = (commandName, commandHandler, permission = "") => {
                  if (typeof permission !== "string") permission = "";
                  scriptManager.registerCommand(commandName.toLowerCase(), commandHandler, currentScriptName, scriptEngine, permission);
                };
                
                const importLib = libName => {
                  return _libImporter.getLib(libName)
                };
                
                const LoadScript = scriptName => {
                  const result = scriptManager.loadScript(new java.io.File(plugin.getDataFolder() + '/scripts/' + scriptName), true);
                  const success = result.isSuccess();
                  const err = result.getMessage();
                  if (!success) {
                    log.error(err);
                  }
                };
                
                const UnloadScript = scriptName => {
                  scriptManager.unloadScript(scriptName);
                };
                
                const setShared = (key, value) => {
                  publicVarManager.setPublicVar(key, value);
                };
                
                const getShared = key => {
                  try {
                    return publicVarManager.getPublicVar(key);
                  } catch (e) {
                    log.warn('Failed to get public variable: ' + e.message);
                    return null;
                  }
                };
                
                const loadVar = (varName, defaultVar, global) =>
                  JSON.parse(variableStorage.getStoredVar(currentScriptName, varName, defaultVar, global));
                
                const saveVar = (varName, variable, global) =>
                  variableStorage.setStoredVar(currentScriptName, varName, JSON.stringify(variable), global);
                
                const getMethod = (Package, MethodName, ExpectedParameters) => {
                  const Methods = Package.getMethods();
                  for (let i = 0; i < Methods.length; i++) {
                    const method = Methods[i];
                    if (method.getName() === MethodName) {
                      if (ExpectedParameters) {
                        const paramCount = method.getParameterCount();
                        if (paramCount === ExpectedParameters.length) {
                          const paramTypes = method.getParameterTypes();
                          let matches = true;
                          for (let j = 0; j < paramCount; j++) {
                            if (paramTypes[j].getName() !== ExpectedParameters[j]) {
                              matches = false;
                              break;
                            }
                          }
                          if (matches) return method;
                        }
                      } else {
                        return method;
                      }
                    }
                  }
                  return null;
                };
                const getMethods = (clazz) => {
                  const methods = clazz.getMethods();
                  for (let i = 0; i < methods.length; i++) {
                    const method = methods[i];
                    const paramTypes = method.getParameterTypes();
                    let paramStr = "";
                    for (let j = 0; j < paramTypes.length; j++) {
                      paramStr += paramTypes[j].getName();
                      if (j < paramTypes.length - 1) paramStr += ", ";
                    }
                    log.info("Method name: " + method.getName());
                    log.info("Method parameters: " + (paramStr || "none"));
                  }
                };
                const DiskApi = Object.freeze({
                  loadFile(fileName, async, global) {
                    DiskStorage.loadFile(fileName, async, currentScriptName, global);
                  },
                  saveFile(fileName, async, global) {
                    DiskStorage.saveFile(fileName, async, currentScriptName, global);
                  },
                  getVar(fileName, valueName, fallbackValue, global) {
                    let rawData = DiskStorage.getValue(currentScriptName, global, fileName, valueName, fallbackValue)
                    if (rawData) {
                        return JSON.parse(rawData)
                    } else {
                        return fallbackValue
                    }
                  },
                  setVar(fileName, valueName, value, global) {
                    DiskStorage.setValue(currentScriptName, global, fileName, valueName, JSON.stringify(value));
                  }
                });

                const task = Object.freeze({
                  wait(seconds) {
                    const continueRunning = _task.wait(currentScriptName, scriptEngine, parseFloat(seconds));
                    if (!continueRunning) {
                      throw new Error('%s');
                    }
                  },
                  waitForScript(scriptName) {
                    _task.waitForScript(scriptName);
                  },
                  waitForPlugin(pluginName) {
                    _task.waitForPlugin(pluginName, currentScriptName);
                  },
                  cancel(taskId) {
                    _task.cancel(currentScriptName, taskId);
                  },
                  spawn(func) {
                    return _task.spawn(currentScriptName, scriptEngine, { f: func });
                  },
                  main(func) {
                    return _task.main(currentScriptName, scriptEngine, { f: func });
                  },
                  entitySchedule(entity, func) {
                    return _task.entitySchedule(currentScriptName, scriptEngine, entity, { f: func });
                  },
                  delay(delay, func) {
                    return _task.delay(currentScriptName, scriptEngine, parseFloat(delay), { f: func });
                  },
                  repeat(delay, period, func) {
                    return _task.repeat(currentScriptName, scriptEngine, parseFloat(delay), parseFloat(period), { f: func });
                  },
                  createListener(javaInterface, handlerObj, gcSet) {
                    let isActive = true;
                    let wrappedHandler = {};
                
                    if (gcSet) {
                      for (const key in handlerObj) {
                        const original = handlerObj[key];
                        if (typeof original === "function") {
                          wrappedHandler[key] = function() {
                            if (gcSet[key] && !isActive) return;
                            return original.apply(this, arguments);
                          };
                        } else {
                          wrappedHandler[key] = original;
                        }
                      }
                    } else {
                      wrappedHandler = handlerObj;
                      log.warn("No garbage collection instructions provided, all methods will never cleanup!");
                    }
                
                    const listener = _task.createListener(currentScriptName, scriptEngine, javaInterface, wrappedHandler);
                
                    _task.setListenerCleanup(currentScriptName, scriptEngine, listener, {
                      f: () => {
                        isActive = false;
                      }
                    });
                
                    return listener;
                  }
                });
                """, pluginLogger.yieldKill);

    public static String JAVASCRIPT_CODE = MAIN_JAVASCRIPT_CODE;

    public static void updateSource() {
        JAVASCRIPT_CODE = MAIN_JAVASCRIPT_CODE +
                (sharedClass.configUtil.getConfigFromBuffer("LoadCustomEventsHandler", true)
                        ?
                        """
                            const registerEvent = function(eventClass, handler) {
                               var wrappedHandler;
                               if (typeof handler === 'function') {
                                   wrappedHandler = { handleEvent: handler };
                               } else if (handler && typeof handler.handleEvent === 'function') {
                                   wrappedHandler = handler;
                               } else {
                                   log.error('Invalid handler: must be a function or an object with a handleEvent method.');
                               }
                               return scriptManager.registerEvent(eventClass, wrappedHandler, currentScriptName, scriptEngine);
                            };
                            const unregisterEvent = function(Listener) {
                                scriptManager.unregisterListener(Listener, currentScriptName)
                            }
                            """
                        : "") +
                (sharedClass.configUtil.getConfigFromBuffer("LoadCustomScheduler", true)
                        ? // TODO: Remove in next major update
                        "function registerSchedule(delay, period, handler, method) {" +
                            "scriptManager.registerSchedule(currentScriptName, delay, period, handler, scriptEngine, method);" +
                        "}"
                        : "");
    }
}