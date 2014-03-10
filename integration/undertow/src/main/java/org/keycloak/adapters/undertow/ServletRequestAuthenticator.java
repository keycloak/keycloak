package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletRequestAuthenticator extends UndertowRequestAuthenticator {

    protected UndertowUserSessionManagement userSessionManagement;

    public ServletRequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, int sslRedirectPort,
                                       SecurityContext securityContext, HttpServerExchange exchange,
                                       UndertowUserSessionManagement userSessionManagement) {
        super(facade, deployment, sslRedirectPort, securityContext, exchange);
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    protected boolean isCached() {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(false);
        if (session == null) {
            log.info("session was null, returning null");
            return false;
        }
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
        if (account == null) {
            log.info("Account was not in session, returning null");
            return false;
        }
        if (account.isActive(deployment)) {
            log.info("Cached account found");
            securityContext.authenticationComplete(account, "KEYCLOAK", false);
            propagateKeycloakContext( account);
            return true;
        }
        log.info("Account was not active, returning null");
        session.setAttribute(KeycloakUndertowAccount.class.getName(), null);
        return false;
    }

    @Override
    protected void propagateKeycloakContext(KeycloakUndertowAccount account) {
        super.propagateKeycloakContext(account);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakSecurityContext.class.getName(), account.getKeycloakSecurityContext());
    }

    @Override
    protected void login(KeycloakUndertowAccount account) {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(true);
        session.setAttribute(KeycloakUndertowAccount.class.getName(), account);
        userSessionManagement.login(servletRequestContext.getDeployment().getSessionManager(), session, account.getPrincipal().getName());

    }
}
