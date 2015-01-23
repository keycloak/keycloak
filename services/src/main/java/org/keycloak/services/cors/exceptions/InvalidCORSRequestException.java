package org.keycloak.services.cors.exceptions;

/**
 * Invalid CORS request exception. Thrown to indicate a CORS request (simple / actual or preflight) that doesn't conform to the
 * specification.
 *
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class InvalidCORSRequestException extends CORSException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new invalid CORS request exception with the specified message.
     *
     * @param message The message.
     */
    public InvalidCORSRequestException(final String message) {

        super(message);
    }
}
