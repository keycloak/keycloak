package org.keycloak.authentication;

/**
 * Throw this exception from an Authenticator, FormAuthenticator, or FormAction if you want to completely abort the flow.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticationFlowException extends RuntimeException {
    private AuthenticationFlowError error;

    public AuthenticationFlowException(AuthenticationFlowError error) {
        this.error = error;
    }

    public AuthenticationFlowException(String message, AuthenticationFlowError error) {
        super(message);
        this.error = error;
    }

    public AuthenticationFlowException(String message, Throwable cause, AuthenticationFlowError error) {
        super(message, cause);
        this.error = error;
    }

    public AuthenticationFlowException(Throwable cause, AuthenticationFlowError error) {
        super(cause);
        this.error = error;
    }

    public AuthenticationFlowException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, AuthenticationFlowError error) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.error = error;
    }

    public AuthenticationFlowError getError() {
        return error;
    }
}
