package org.keycloak.services.cors.exceptions;

/**
 * Unsupported HTTP header exception. Thrown to indicate that a custom HTTP request header is not supported by the CORS policy.
 *
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class UnsupportedHTTPHeaderException extends CORSException {

    private static final long serialVersionUID = 1L;
    /**
     * The HTTP header.
     */
    private final String header;

    /**
     * Creates a new unsupported HTTP header exception with the specified message.
     *
     * @param message The message.
     */
    public UnsupportedHTTPHeaderException(final String message) {

        this(message, null);
    }

    /**
     * Creates a new unsupported HTTP header exception with the specified message and request header.
     *
     * @param message The message.
     * @param requestHeader The request HTTP header name, {@code null} if unknown.
     */
    public UnsupportedHTTPHeaderException(final String message, final String requestHeader) {

        super(message);
        header = requestHeader;
    }

    /**
     * Gets the request header name.
     *
     * @return The request header name, {@code null} if unknown or not set.
     */
    public String getRequestHeader() {

        return header;
    }
}
