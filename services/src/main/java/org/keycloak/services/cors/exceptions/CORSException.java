package org.keycloak.services.cors.exceptions;

/**
 * Base Cross-Origin Resource Sharing (CORS) exception, typically thrown during processing of CORS requests.
 *
 * @author <a href="mailto:giriraj.sharma27@gmail.com">Giriraj Sharma</a>
 */
public class CORSException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CORS exception with the specified message.
     *
     * @param message The message.
     */
    public CORSException(final String message) {

        super(message);
    }
}
