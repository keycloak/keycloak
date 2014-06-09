package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.Sessions;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public abstract class UndertowRequestAuthenticator extends RequestAuthenticator {
    protected SecurityContext securityContext;
    protected HttpServerExchange exchange;
    protected UndertowUserSessionManagement userSessionManagement;


    public UndertowRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                        SecurityContext securityContext, HttpServerExchange exchange,
                                        UndertowUserSessionManagement userSessionManagement) {
        super(facade, deployment, sslRedirectPort);
        this.securityContext = securityContext;
        this.exchange = exchange;
        this.userSessionManagement = userSessionManagement;
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
        KeycloakUndertowAccount account = createAccount(principal, session);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(account);
        login(account);
    }

    protected void login(KeycloakAccount account) {
        Session session = Sessions.getOrCreateSession(exchange);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        String username = account.getPrincipal().getName();
        String keycloakSessionId = account.getKeycloakSecurityContext().getToken().getSessionState();
        userSessionManagement.login(session.getSessionManager(), session.getId(), username, keycloakSessionId);
    }


    @Override
    protected void completeBearerAuthentication(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session) {
        KeycloakUndertowAccount account = createAccount(principal, session);
        securityContext.authenticationComplete(account, "KEYCLOAK", false);
        propagateKeycloakContext(account);
    }

    @Override
    protected boolean isCached() {
        Session session = Sessions.getSession(exchange);
        if (session == null) {
            log.info("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.info("Account was not in session, returning null");
            return false;
        }
        account.setDeployment(deployment);
        if (account.isActive()) {
            log.info("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            propagateKeycloakContext( account);
            return true;
        }
        log.info("Account was not active, returning false");
        session.removeAttribute(KeycloakUndertowAccount.class.getName());
        return false;
    }

    /**
     * Subclasses need to be able to create their own version of the KeycloakUndertowAccount
     * @return The account
     */
    protected abstract KeycloakUndertowAccount createAccount(KeycloakPrincipal principal, RefreshableKeycloakSecurityContext session);
}
