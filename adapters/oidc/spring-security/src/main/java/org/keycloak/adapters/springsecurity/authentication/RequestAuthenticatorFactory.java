package org.keycloak.adapters.springsecurity.authentication;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * Creates {@link RequestAuthenticator}s.
 */
public interface RequestAuthenticatorFactory {
    /**
     * Creates new {@link RequestAuthenticator} instances on a per-request basis.
    */
    RequestAuthenticator createRequestAuthenticator(HttpFacade facade, HttpServletRequest request, 
            KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort);
}