package org.freedesktop.dbus.interfaces;

import org.freedesktop.dbus.messages.DBusSignal;

/**
 * Handle a signal on DBus. All Signal handlers are run in their own Thread. Application writers are responsible for
 * managing any concurrency issues.
 */
public interface DBusSigHandler<T extends DBusSignal> {
    /**
     * Handle a signal.
     *
     * @param _signal The signal to handle. If such a class exists, the signal will be an instance of the class with the
     *            correct type signature. Otherwise it will be an instance of DBusSignal
     */
    void handle(T _signal);
}
