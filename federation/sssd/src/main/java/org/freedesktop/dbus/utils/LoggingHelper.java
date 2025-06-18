package org.freedesktop.dbus.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Helper for some logging stuff, e.g. avoid call {@link Arrays#deepToString(Object[])} if loglevel is not enabled.
 *
 * @author David M.
 * @since v3.2.4 - 2020-08-24
 */
public final class LoggingHelper {

    private LoggingHelper() {
    }

    /**
     * Creates a toString() result for an array.
     * Will resolve nested arrays and collections.
     *
     * @param _array array to convert
     * @return String or null if input null
     *
     * @since v4.2.2 - 2023-01-20
     */
    public static String arraysVeryDeepString(Object[] _array) {
        if (_array == null) {
            return null;
        }

        return String.join(", ", arraysVeryDeepStringRecursive(_array));
    }

    /**
     * Creates a toString() result for an array.
     * Will resolve nested arrays and collections.
     *
     * @param _array array to convert
     * @return List of String, null if input null
     *
     * @since v4.2.2 - 2023-01-20
     */
    private static List<String> arraysVeryDeepStringRecursive(Object[] _array) {
        if (_array == null) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (Object object : _array) {
            if (object == null) {
                result.add("(null)");
            } else if (object.getClass().isArray()) {
                result.add(arraysVeryDeepStringRecursive((Object[]) object).toString());
            } else if (object instanceof Collection<?>) {
                Collection<?> c = (Collection<?>) object;
                result.add(arraysVeryDeepStringRecursive(c.toArray()).toString());
            } else {
                result.add(Objects.toString(object));
            }
        }

        return result;
    }

    /**
     * Executes the runnable if the boolean is true.
     *
     * @param _enabled boolean, if true runnable is executed
     * @param _loggerCall runnable containing logger call
     */
    public static void logIf(boolean _enabled, Runnable _loggerCall) {
        if (_enabled) {
            _loggerCall.run();
        }
    }

}
