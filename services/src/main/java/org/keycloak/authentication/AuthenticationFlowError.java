package org.keycloak.authentication;

/**
 * Set of error codes that can be thrown by an Authenticator, FormAuthenticator, or FormAction
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum AuthenticationFlowError {
    EXPIRED_CODE,
    INVALID_CLIENT_SESSION,
    INVALID_USER,
    INVALID_CREDENTIALS,
    CREDENTIAL_SETUP_REQUIRED,
    USER_DISABLED,
    USER_CONFLICT,
    USER_TEMPORARILY_DISABLED,
    INTERNAL_ERROR,
    UNKNOWN_USER,
    FORK_FLOW,
    UNKNOWN_CLIENT,
    CLIENT_NOT_FOUND,
    CLIENT_DISABLED,
    CLIENT_CREDENTIALS_SETUP_REQUIRED,
    INVALID_CLIENT_CREDENTIALS
}
