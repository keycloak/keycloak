package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowRequestAuthenticator extends RequestAuthenticator {
    protected SecurityContext securityContext;
    protected HttpServerExchange exchange;


    public UndertowRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort, SecurityContext securityContext, HttpServerExchange exchange) {
        super(facade, deployment, sslRedirectPort);
        this.securityContext = securityContext;
        this.exchange = exchange;
    }

    protected void propagateKeycloakContext(KeycloakUndertowAccount account) {
        exchange.putAttachment(UndertowHttpFacade.KEYCLOAK_SECURITY_CONTEXT_KEY, account.getKeycloakSecurityContext());
    }

    @Override
    protected OAuthRequestAuthenticator createOAuthAuthenticator() {
        return new OAuthRequestAuthenticator(facade, deployment, sslRedirectPort) {
            @Override
            protected void saveRequest() {
                // todo
            }
        };
    }

    @Override
    protected void completeOAuthAuthentication(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session) {
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, session, deployment);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(account);
        login(account);
    }

    protected void login(KeycloakUndertowAccount account) {

    }


    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session) {
        KeycloakUndertowAccount account = new KeycloakUndertowAccount(principal, session, deployment);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(account);
    }

    @Override
    protected boolean isCached() {
        return false;
    }
}
