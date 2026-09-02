package org.keycloak.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A utility class for working with {@link Throwable} instances.
 */
public final class Throwables {

    /**
     * Checks if the given {@code throwable} or any of its causes (up to a depth of 3) is an instance of any of the specified exception types.
     *
     * @param throwable
     * @param type
     * @return
     */
    @SafeVarargs
    public static boolean isCausedBy(Throwable throwable, Class<? extends Exception>... type) {
        Objects.requireNonNull(throwable, "Throwable must not be null");
        int limit = 3;
        List<Class<? extends Exception>> types = Arrays.asList(type);
        Throwable cause = throwable.getCause();

        while (cause != null) {
            if (limit-- == 0) {
                break;
            }
            if (types.contains(cause.getClass())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }
}
