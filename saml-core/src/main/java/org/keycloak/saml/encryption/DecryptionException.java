package org.keycloak.saml.encryption;

public class DecryptionException extends Exception {
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7785660781162212790L;

    public DecryptionException() {
        super();
    }

    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecryptionException(Throwable cause) {
        super(cause);
    }

    public DecryptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
