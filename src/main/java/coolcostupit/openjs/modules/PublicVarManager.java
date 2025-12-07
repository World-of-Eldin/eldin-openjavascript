/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.modules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PublicVarManager {
    private final Map<String, Object> publicVars = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();
    private final Condition varAvailable = lock.newCondition();

    public void setPublicVar(String key, Object value) {
        lock.lock();
        try {
            publicVars.put(key, value);
            varAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public Object getPublicVar(String key) throws InterruptedException {
        lock.lock();
        try {
            long timeout = TimeUnit.MILLISECONDS.toNanos(500);
            while (!publicVars.containsKey(key)) {
                if (timeout <= 0L) {
                    return null; // Return null if timeout occurs
                }
                timeout = varAvailable.awaitNanos(timeout);
            }
            return publicVars.get(key);
        } finally {
            lock.unlock();
        }
    }
}
