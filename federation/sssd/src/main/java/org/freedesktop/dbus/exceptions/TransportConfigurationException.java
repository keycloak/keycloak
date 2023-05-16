package org.freedesktop.dbus.exceptions;

public class TransportConfigurationException extends Exception {
    private static final long serialVersionUID = 1L;

    public TransportConfigurationException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public TransportConfigurationException(String _message) {
        super(_message);
    }

}
