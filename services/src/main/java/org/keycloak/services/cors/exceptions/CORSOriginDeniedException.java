package org.keycloak.services.cors.exceptions;

/**
 * CORS origin denied (not allowed) exception.
 *
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class CORSOriginDeniedException extends CORSException {

    private static final long serialVersionUID = 1L;

    /**
     * The request origin.
     */
    private final String requestOrigin;

    /**
     * Creates a new CORS origin denied exception with the specified message.
     *
     * @param message The message.
     */
    public CORSOriginDeniedException(final String message) {

        this(message, null);
    }

    /**
     * Creates a new CORS origin denied exception with the specified message and request origins.
     *
     * @param message The message.
     * @param requestOrigin The request origin, {@code null} if unknown.
     */
    public CORSOriginDeniedException(final String message, final String requestOrigin) {

        super(message);
        this.requestOrigin = requestOrigin;
    }

    /**
     * Gets the request origin.
     *
     * @return The request origin, {@code null} if unknown or not set.
     */
    public String getRequestOrigin() {

        return requestOrigin;
    }
}
