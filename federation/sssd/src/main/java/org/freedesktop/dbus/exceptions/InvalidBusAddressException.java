package org.freedesktop.dbus.exceptions;

/**
 * Thrown when a invalid BusAddress should be created.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-18
 */
public class InvalidBusAddressException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public InvalidBusAddressException() {
        super();
    }

    public InvalidBusAddressException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public InvalidBusAddressException(String _s) {
        super(_s);
    }

    public InvalidBusAddressException(Throwable _cause) {
        super(_cause);
    }

}
