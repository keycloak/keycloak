package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthenticationMechanism extends KeycloakAuthenticationMechanism {
    protected ConfidentialPortManager portManager;
    protected UserSessionManagement userSessionManagement;

    public ServletKeycloakAuthenticationMechanism(UserSessionManagement userSessionManagement, AdapterConfig config, RealmConfiguration realmConfig, ConfidentialPortManager portManager) {
        super(config, realmConfig);
        this.portManager = portManager;
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new ServletOAuthAuthenticator(exchange, realmConfig, portManager);
    }

    @Override
    protected KeycloakUndertowAccount checkCachedAccount(HttpServerExchange exchange) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(false);
        if (session == null) {
            log.info("session was null, returning null");
            return null;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.info("Account was not in session, returning null");
            return null;
        }
        if (account.isActive(realmConfig, adapterConfig)) return account;
        log.info("Account was not active, returning null");
        session.setAttribute(KeycloakUndertowAccount.class.getName(), null);
        return null;
    }

    @Override
    protected void propagateKeycloakContext(HttpServerExchange exchange, KeycloakUndertowAccount account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakSecurityContext.class.getName(), account.getSession());
    }



    @Override
    protected void login(HttpServerExchange exchange, KeycloakUndertowAccount account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakSecurityContext.class.getName(), account.getSession());
        HttpSession session = req.getSession(true);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        userSessionManagement.login(servletRequestContext.getDeployment().getSessionManager(), session, account.getPrincipal().getName());

    }
}
