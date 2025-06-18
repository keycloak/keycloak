package org.freedesktop.dbus.exceptions;

import java.io.IOException;

/**
 * Exception which indicates a terminated connection.
 *
 * @author hypfvieh
 * @since v4.2.2 - 2023-02-01
 */
public class SocketClosedException extends IOException {
    private static final long serialVersionUID = 1L;

    public SocketClosedException() {
        super();
    }

    public SocketClosedException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public SocketClosedException(String _message) {
        super(_message);
    }

    public SocketClosedException(Throwable _cause) {
        super(_cause);
    }

}
