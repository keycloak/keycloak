package org.keycloak.authentication;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationProviderException extends Exception {

    private static final long serialVersionUID = 15L;

    protected AuthenticationProviderException() {
    }

    public AuthenticationProviderException(String message) {
        super(message);
    }

    public AuthenticationProviderException(Throwable cause) {
        super(cause);
    }

    public AuthenticationProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
