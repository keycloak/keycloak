package org.keycloak.adapters.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.adapters.config.RealmConfiguration;
import org.keycloak.ResourceMetadata;
import org.keycloak.SkeletonKeySession;
import org.keycloak.adapters.config.AdapterConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthenticationMechanism extends KeycloakAuthenticationMechanism {
    protected ConfidentialPortManager portManager;

    public ServletKeycloakAuthenticationMechanism(ResourceMetadata resourceMetadata, AdapterConfig config, RealmConfiguration realmConfig, ConfidentialPortManager portManager) {
        super(resourceMetadata, config, realmConfig);
        this.portManager = portManager;
    }

    @Override
    protected OAuthAuthenticator createOAuthAuthenticator(HttpServerExchange exchange) {
        return new ServletOAuthAuthenticator(exchange, realmConfig, portManager);
    }

    @Override
    protected void propagateBearer(HttpServerExchange exchange, SkeletonKeySession session) {
        super.propagateBearer(exchange, session);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(SkeletonKeySession.class.getName(), session);
    }

    @Override
    protected void propagateOauth(HttpServerExchange exchange, SkeletonKeySession skSession) {
        super.propagateOauth(exchange, skSession);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(SkeletonKeySession.class.getName(), skSession);
        HttpSession session = req.getSession(true);
        session.setAttribute(SkeletonKeySession.class.getName(), skSession);
    }
}
