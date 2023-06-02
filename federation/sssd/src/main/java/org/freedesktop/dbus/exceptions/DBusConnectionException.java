package org.freedesktop.dbus.exceptions;

/**
 * Thrown when something goes wrong with the connection to DBus.<p>
 * This includes find the connection parameter (e.g. machine-id file) or establishing the connection.
 *
 * @author David M.
 * @since v3.3.0 - 2021-01-27
 */
public class DBusConnectionException extends DBusException {
    private static final long serialVersionUID = -1L;

    public DBusConnectionException() {
        super();
    }

    public DBusConnectionException(String _message, Throwable _cause, boolean _enableSuppression,
            boolean _writableStackTrace) {
        super(_message, _cause, _enableSuppression, _writableStackTrace);
    }

    public DBusConnectionException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public DBusConnectionException(String _message) {
        super(_message);
    }

    public DBusConnectionException(Throwable _cause) {
        super(_cause);
    }

}
