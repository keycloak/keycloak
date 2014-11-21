package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.undertow.AbstractUndertowRequestAuthenticator;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowRequestAuthenticator extends AbstractUndertowRequestAuthenticator {
    public UndertowRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                        SecurityContext securityContext, HttpServerExchange exchange, AdapterTokenStore tokenStore) {
        super(facade, deployment, sslRedirectPort, securityContext, exchange, tokenStore);
    }

    @Override
    protected KeycloakUndertowAccount createAccount(KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal) {
        return new KeycloakUndertowAccount(principal);
    }
}
