package org.freedesktop.dbus.exceptions;

/**
 * Thrown if registration of transport providers fails.
 *
 * @author hypfvieh
 * @since v4.0.0 - 2021-09-08
 */
public class TransportRegistrationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TransportRegistrationException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public TransportRegistrationException(String _message) {
        super(_message);
    }

}
