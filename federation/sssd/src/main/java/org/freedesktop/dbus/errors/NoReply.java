package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if there is no reply to a method call
 */
public class NoReply extends DBusExecutionException {
    private static final long serialVersionUID = 5280031560938871837L;

    public NoReply(String _message) {
        super(_message);
    }
}
