package org.freedesktop.dbus.exceptions;

/**
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public class InvalidObjectPathException extends DBusException {
    private static final long serialVersionUID = 1L;

    public InvalidObjectPathException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public InvalidObjectPathException(String _busName) {
        super("Invalid object path: " + _busName);
    }

    public InvalidObjectPathException(Throwable _cause) {
        super(_cause);
    }

    public InvalidObjectPathException() {
        super((String) null);
    }
}
