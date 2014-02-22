package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.KeycloakAuthenticatedSession;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.adapters.ResourceMetadata;
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

    public ServletKeycloakAuthenticationMechanism(AdapterConfig config, ResourceMetadata metadata, ConfidentialPortManager portManager) {
        super(config, metadata);
        this.portManager = portManager;
    }


    @Override
    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new ServletOAuthAuthenticator(exchange, realmConfig, portManager);
    }

    /*
    @Override
    protected void propagateBearer(HttpServerExchange exchange, KeycloakAuthenticatedSession skSession, KeycloakPrincipal principal) {
        super.propagateBearer(exchange, skSession, principal);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakAuthenticatedSession.class.getName(), skSession);
    }

    @Override
    protected void propagateOauth(HttpServerExchange exchange, KeycloakAuthenticatedSession skSession, KeycloakPrincipal principal) {
        super.propagateBearer(exchange, skSession, principal);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakAuthenticatedSession.class.getName(), skSession);
        HttpSession session = req.getSession(true);
        session.setAttribute(KeycloakAuthenticatedSession.class.getName(), skSession);
        userSessionManagement.login(servletRequestContext.getDeployment().getSessionManager(), session, principal.getName());
    }
    */
}
