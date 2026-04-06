package org.freedesktop.dbus.exceptions;

/**
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public class InvalidBusNameException extends DBusException {
    private static final long serialVersionUID = 1L;

    public InvalidBusNameException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public InvalidBusNameException(String _busName) {
        super("Invalid bus name: " + _busName);
    }

    public InvalidBusNameException(Throwable _cause) {
        super(_cause);
    }

    public InvalidBusNameException() {
        super((String) null);
    }
}
