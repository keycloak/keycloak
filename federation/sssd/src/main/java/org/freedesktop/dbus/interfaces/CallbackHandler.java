package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Interface for callbacks in async mode
 */
public interface CallbackHandler<T> {
    void handle(T _r);

    void handleError(DBusExecutionException _ex);
}
