package org.keycloak.adapters.wildfly;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.ServletKeycloakAuthMech;
import org.keycloak.adapters.undertow.ServletRequestAuthenticator;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WildflyAuthenticationMechanism extends ServletKeycloakAuthMech {

    public WildflyAuthenticationMechanism(AdapterDeploymentContext deploymentContext,
                                          UndertowUserSessionManagement userSessionManagement,
                                          NodesRegistrationManagement nodesRegistrationManagement,
                                          ConfidentialPortManager portManager) {
        super(deploymentContext, userSessionManagement, nodesRegistrationManagement, portManager);
    }

    @Override
    protected ServletRequestAuthenticator createRequestAuthenticator(KeycloakDeployment deployment, HttpServerExchange exchange, SecurityContext securityContext, UndertowHttpFacade facade) {
        int confidentialPort = getConfidentilPort(exchange);
        AdapterTokenStore tokenStore = getTokenStore(exchange, facade, deployment, securityContext);
        return new WildflyRequestAuthenticator(facade, deployment,
                confidentialPort, securityContext, exchange, tokenStore);
    }
}
