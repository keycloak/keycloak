package org.freedesktop.dbus.exceptions;

/**
 * An exception within DBus.
 */
public class DBusException extends Exception {
    private static final long serialVersionUID = -1L;

    /**
    * Create an exception with the specified message
    * @param _message message
    */
    public DBusException(String _message) {
        super(_message);
    }

    public DBusException() {
        super();
    }

    public DBusException(String _message, Throwable _cause, boolean _enableSuppression, boolean _writableStackTrace) {
        super(_message, _cause, _enableSuppression, _writableStackTrace);
    }

    public DBusException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public DBusException(Throwable _cause) {
        super(_cause);
    }
}
