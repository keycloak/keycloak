package org.keycloak.adapters.wildfly;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.undertow.ServletKeycloakAuthMech;
import org.keycloak.adapters.undertow.ServletRequestAuthenticator;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflyAuthenticationMechanism extends ServletKeycloakAuthMech {

    public WildflyAuthenticationMechanism(KeycloakDeployment deployment,
                                          UndertowUserSessionManagement userSessionManagement,
                                          ConfidentialPortManager portManager) {
        super(deployment, userSessionManagement, portManager);
    }

    @Override
    protected ServletRequestAuthenticator createRequestAuthenticator(HttpServerExchange exchange, SecurityContext securityContext, UndertowHttpFacade facade) {
        return new WildflyRequestAuthenticator(facade, deployment,
                portManager.getConfidentialPort(exchange), securityContext, exchange, userSessionManagement);
    }
}
