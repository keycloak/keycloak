package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.FatalException;

public class FatalDBusException extends DBusException implements FatalException {

    private static final long serialVersionUID = -3461692622913793488L;

    public FatalDBusException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public FatalDBusException(Throwable _cause) {
        super(_cause);
    }

    public FatalDBusException(String _message) {
        super(_message);
    }
}
