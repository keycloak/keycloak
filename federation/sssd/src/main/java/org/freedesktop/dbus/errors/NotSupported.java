package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a called operation is not supported
 */
public class NotSupported extends DBusExecutionException {
    private static final long serialVersionUID = -3937521136197720266L;

    public NotSupported(String _message) {
        super(_message);
    }
}
