package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletSamlAuthMech extends AbstractSamlAuthMech {
    public ServletSamlAuthMech(SamlDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement,
                               String logoutPage, String errorPage) {
        super(deploymentContext, sessionManagement, logoutPage, errorPage);
    }

    @Override
    protected SamlSessionStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, SamlDeployment deployment, SecurityContext securityContext) {
        return new ServletSamlSessionStore(exchange, sessionManagement, securityContext);
    }
}
