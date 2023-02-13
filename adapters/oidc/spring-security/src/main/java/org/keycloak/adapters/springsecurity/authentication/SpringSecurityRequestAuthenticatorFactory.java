package org.keycloak.adapters.springsecurity.authentication;

import javax.servlet.http.HttpServletRequest;

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

public class SpringSecurityRequestAuthenticatorFactory implements RequestAuthenticatorFactory {
    
    @Override
    public RequestAuthenticator createRequestAuthenticator(HttpFacade facade,
            HttpServletRequest request, KeycloakDeployment deployment, AdapterTokenStore tokenStore, int sslRedirectPort) {
        return new SpringSecurityRequestAuthenticator(facade, request, deployment, tokenStore, sslRedirectPort);
    }
}