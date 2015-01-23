package org.keycloak.services.cors.exceptions;

/**
 * Unsupported HTTP method exception.
 *
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class UnsupportedHTTPMethodException extends CORSException {

    private static final long serialVersionUID = 1L;
    /**
     * The requested HTTP method.
     */
    private final String method;

    /**
     * Creates a new unsupported HTTP method exception with the specified message.
     *
     * @param message The message.
     */
    public UnsupportedHTTPMethodException(final String message) {

        this(message, null);
    }

    /**
     * Creates a new unsupported HTTP method exception with the specified message and requested method.
     *
     * @param message The message.
     * @param requestedMethod The requested HTTP method, {@code null} if unknown.
     */
    public UnsupportedHTTPMethodException(final String message, final String requestedMethod) {

        super(message);
        method = requestedMethod;
    }

    /**
     * Gets the requested method.
     *
     * @return The requested method, {@code null} if unknown or not set.
     */
    public String getRequestedMethod() {

        return method;
    }
}
