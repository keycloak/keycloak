package org.freedesktop.dbus.exceptions;

/**
 * Thrown when a used thread pool (e.g. in ReceivingService) is in an invalid state.
 *
 * @author hypfvieh
 * @since 4.2.0 - 2022-07-14
 */
public class IllegalThreadPoolStateException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public IllegalThreadPoolStateException() {
        super();
    }

    public IllegalThreadPoolStateException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

    public IllegalThreadPoolStateException(String _s) {
        super(_s);
    }

    public IllegalThreadPoolStateException(Throwable _cause) {
        super(_cause);
    }

}
