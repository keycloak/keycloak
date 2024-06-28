package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

public class InternalMessageException extends DBusExecutionException implements NonFatalException {
    private static final long serialVersionUID = 1L;

    public InternalMessageException(String _message) {
        super(_message);
    }
}
