package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a interface does not exist
 */
public class UnknownInterface extends DBusExecutionException {
    private static final long serialVersionUID = -6296696668185701195L;

    public UnknownInterface(String _message) {
        super(_message);
    }
}
