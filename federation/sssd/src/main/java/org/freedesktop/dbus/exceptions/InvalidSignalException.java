package org.freedesktop.dbus.exceptions;

/**
 * @since 5.0.0 - 2023-11-08
 * @author hypfvieh
 */
public class InvalidSignalException extends DBusException {
    private static final long serialVersionUID = 1L;

    public InvalidSignalException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public InvalidSignalException(String _message) {
        super(_message);
    }

    public InvalidSignalException(Class<?> _clz) {
        super(_clz == null ? "Null is not a signal" : _clz.getName() + " is not a signal");
    }

    public InvalidSignalException(Throwable _cause) {
        super(_cause);
    }

}
