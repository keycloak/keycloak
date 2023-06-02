package org.freedesktop.dbus.utils;

/**
 * Supplier which allows throwing any exception.
 *
 * @param <V> type which is supplied
 * @param <T> type of exception which gets thrown
 *
 * @author hypfvieh
 * @since v1.3.0 - 2023-01-12
 */
@FunctionalInterface
public interface IThrowingSupplier<V, T extends Throwable> {
    /**
     * Returns the result of the supplier or throws an exception.
     *
     * @return result of supplied function
     * @throws T exception
     */
    V get() throws T;
}
