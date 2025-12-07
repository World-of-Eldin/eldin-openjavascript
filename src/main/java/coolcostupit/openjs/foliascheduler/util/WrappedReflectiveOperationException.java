/*
 * Copyright (c) 2025 coolcostupit
 * Licensed under AGPL-3.0
 * You may not remove this notice or claim this work as your own.
 */

package coolcostupit.openjs.foliascheduler.util;

import org.jetbrains.annotations.NotNull;

/**
 * Wraps a {@link ReflectiveOperationException} in a {@link RuntimeException}.
 *
 * <p>By wrapping checked exceptions in a runtime exception, the caller does not
 * need to handle the checked exception. This is useful when using reflection
 * and the caller does not want to handle checked exceptions.
 */
public class WrappedReflectiveOperationException extends RuntimeException {
    public WrappedReflectiveOperationException(@NotNull ReflectiveOperationException e) {
        super(e);
    }
}
