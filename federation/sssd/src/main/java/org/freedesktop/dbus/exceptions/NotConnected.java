package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

/**
 * Thrown if a DBus action is called when not connected to the Bus.
 */
public class NotConnected extends DBusExecutionException implements FatalException {
    private static final long serialVersionUID = -3566138179099398537L;

    public NotConnected(String _message) {
        super(_message);
    }
}
