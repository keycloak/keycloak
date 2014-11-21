package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.Sessions;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakAccount;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Per-request object. Storage of tokens in undertow session.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UndertowSessionTokenStore implements AdapterTokenStore {

    protected static Logger log = Logger.getLogger(UndertowSessionTokenStore.class);

    private final HttpServerExchange exchange;
    private final KeycloakDeployment deployment;
    private final UndertowUserSessionManagement sessionManagement;
    private final SecurityContext securityContext;

    public UndertowSessionTokenStore(HttpServerExchange exchange, KeycloakDeployment deployment, UndertowUserSessionManagement sessionManagement,
                                     SecurityContext securityContext) {
        this.exchange = exchange;
        this.deployment = deployment;
        this.sessionManagement = sessionManagement;
        this.securityContext = securityContext;
    }

    @Override
    public void checkCurrentToken() {
        // no-op on undertow
    }

    @Override
    public boolean isCached(RequestAuthenticator authenticator) {
        Session session = Sessions.getSession(exchange);
        if (session == null) {
            log.debug("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.debug("Account was not in session, returning null");
            return false;
        }

        if (!deployment.getRealm().equals(account.getKeycloakSecurityContext().getRealm())) {
            log.debug("Account in session belongs to a different realm than for this request.");
            return false;
        }

        account.setCurrentRequestInfo(deployment, this);
        if (account.checkActive()) {
            log.debug("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            ((AbstractUndertowRequestAuthenticator)authenticator).propagateKeycloakContext(account);
            return true;
        } else {
            log.debug("Account was not active, returning false");
            session.removeAttribute(KeycloakUndertowAccount.class.getName());
            session.invalidate(exchange);
            return false;
        }
    }

    @Override
    public void saveAccountInfo(KeycloakAccount account) {
        Session session = Sessions.getOrCreateSession(exchange);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        sessionManagement.login(session.getSessionManager());
    }

    @Override
    public void logout() {
        Session session = Sessions.getSession(exchange);
        if (session == null) return;
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) return;
        session.removeAttribute(KeycloakUndertowAccount.class.getName());
    }

    @Override
    public void refreshCallback(RefreshableKeycloakSecurityContext securityContext) {
        // no-op
    }
}
