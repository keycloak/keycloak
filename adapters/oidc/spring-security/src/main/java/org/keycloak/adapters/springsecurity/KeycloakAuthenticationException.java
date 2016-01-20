package org.keycloak.adapters.springsecurity;

import org.springframework.security.core.AuthenticationException;

public class KeycloakAuthenticationException extends AuthenticationException {
    public KeycloakAuthenticationException(String msg, Throwable t) {
        super(msg, t);
    }

    public KeycloakAuthenticationException(String msg) {
        super(msg);
    }
}
