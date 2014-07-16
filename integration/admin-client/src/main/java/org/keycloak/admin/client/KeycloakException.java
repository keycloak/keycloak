package org.keycloak.admin.client;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakException extends RuntimeException {

    public KeycloakException(String message) {
        super(message);
    }

    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeycloakException(Throwable cause) {
        super(cause);
    }

}
