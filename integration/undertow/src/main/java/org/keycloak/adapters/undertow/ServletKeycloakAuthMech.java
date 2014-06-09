package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthMech extends UndertowKeycloakAuthMech {

    protected AdapterDeploymentContext deploymentContext;
    protected UndertowUserSessionManagement userSessionManagement;
    protected ConfidentialPortManager portManager;

    public ServletKeycloakAuthMech(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement, ConfidentialPortManager portManager) {
        this.deploymentContext = deploymentContext;
        this.userSessionManagement = userSessionManagement;
        this.portManager = portManager;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        RequestAuthenticator authenticator = createRequestAuthenticator(deployment, exchange, securityContext, facade);

        return super.keycloakAuthenticate(exchange, authenticator);
    }

    protected RequestAuthenticator createRequestAuthenticator(KeycloakDeployment deployment, HttpServerExchange exchange, SecurityContext securityContext, UndertowHttpFacade facade) {

        int confidentialPort = getConfidentilPort(exchange);
        return new ServletRequestAuthenticator(facade, deployment,
                confidentialPort, securityContext, exchange, userSessionManagement);
    }

    protected int getConfidentilPort(HttpServerExchange exchange) {
        int confidentialPort = 8443;
        if (exchange.getRequestScheme().equalsIgnoreCase("HTTPS")) {
            confidentialPort = exchange.getHostPort();
        } else if (portManager != null) {
            confidentialPort = portManager.getConfidentialPort(exchange);
        }
        return confidentialPort;
    }

}
