package org.freedesktop.dbus.exceptions;

import org.freedesktop.dbus.interfaces.NonFatalException;

public class MarshallingException extends DBusException implements NonFatalException {

    private static final long serialVersionUID = 3065477360622428063L;

    public MarshallingException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public MarshallingException(String _message) {
        super(_message);
    }
}
