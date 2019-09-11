package org.keycloak.vault;

/**
 * Thrown when a vault directory doesn't exist.
 *
 * @author Sebastian Łaskawiec
 */
public class VaultNotFoundException extends RuntimeException {

    /**
     * Constructs new exception.
     *
     * @param message A full text message of the exception.
     */
    public VaultNotFoundException(String message) {
        super(message);
    }
}
