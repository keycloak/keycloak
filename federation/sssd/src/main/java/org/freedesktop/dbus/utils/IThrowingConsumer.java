package org.freedesktop.dbus.utils;

/**
 * Supplier which allows throwing any exception.
 *
 * @param <V> type which is supplied
 * @param <T> type of exception which gets thrown
 *
 * @author hypfvieh
 * @since v5.1.0 - 2024-07-12
 */
@FunctionalInterface
public interface IThrowingConsumer<V, T extends Throwable> {
    /**
     * Performs this operation on the given argument.
     *
     * @throws T exception
     */
    void accept(V _val) throws T;
}
