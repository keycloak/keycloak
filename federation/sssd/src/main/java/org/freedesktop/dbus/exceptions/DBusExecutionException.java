package org.freedesktop.dbus.exceptions;

/**
 * An exception while running a remote method within DBus.
 */
@SuppressWarnings("checkstyle:mutableexception")
public class DBusExecutionException extends RuntimeException {
    private static final long serialVersionUID = 6327661667731344250L;

    private String type;

    /**
    * Create an exception with the specified message
    * @param _message message
    */
    public DBusExecutionException(String _message) {
        super(_message);
    }

    public void setType(String _type) {
        this.type = _type;
    }

    /**
    * Get the DBus type of this exception. Use if this
    * was an exception we don't have a class file for.
    *
    * @return string
    */
    public String getType() {
        if (null == type) {
            return getClass().getName();
        } else {
            return type;
        }
    }
}
