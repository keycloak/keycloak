package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a arguments passed to the method are invalid
 */
public class InvalidMethodArgument extends DBusExecutionException {
    private static final long serialVersionUID = 2504012938615867394L;

    public InvalidMethodArgument(String _message) {
        super(_message);
    }
}
