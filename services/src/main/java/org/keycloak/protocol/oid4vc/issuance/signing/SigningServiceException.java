package org.keycloak.protocol.oid4vc.issuance.signing;

/**
 * Exception to be thrown if credentials signing does fail
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class SigningServiceException extends RuntimeException {

    public SigningServiceException(String message) {
        super(message);
    }

    public SigningServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
