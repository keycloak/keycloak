package org.freedesktop.dbus.exceptions;

/**
 * An exception while running a remote method within DBus.
 */
@SuppressWarnings("checkstyle:mutableexception")
public class DBusExecutionException extends RuntimeException {
    private static final long serialVersionUID = 6327661667731344250L;

    /**
    * Create an exception with the specified message
    * @param _message message
    */
    public DBusExecutionException(String _message) {
        super(_message);
    }

    /**
    * Create an exception with the specified message
    * @param _message message
    * @param _cause cause
    */
    public DBusExecutionException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    /**
     * @deprecated the configured type is never used
     */
    @Deprecated(forRemoval = true, since = "5.1.0 - 2024-07-12")
    public void setType(String _type) {
    }

    /**
    * @deprecated type is never used
    */
    @Deprecated(forRemoval = true, since = "5.1.0 - 2024-07-12")
    public String getType() {
        return getClass().getName();
    }
}
