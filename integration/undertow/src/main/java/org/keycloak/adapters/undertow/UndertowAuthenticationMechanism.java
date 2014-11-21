package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowAuthenticationMechanism extends AbstractUndertowKeycloakAuthMech {
    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected int confidentialPort;

    public UndertowAuthenticationMechanism(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement,
                                           NodesRegistrationManagement nodesRegistrationManagement, int confidentialPort) {
        super(deploymentContext, sessionManagement);
        this.nodesRegistrationManagement = nodesRegistrationManagement;
        this.confidentialPort = confidentialPort;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        nodesRegistrationManagement.tryRegister(deployment);

        AdapterTokenStore tokenStore = getTokenStore(exchange, facade, deployment, securityContext);
        RequestAuthenticator authenticator = new UndertowRequestAuthenticator(facade, deployment, confidentialPort, securityContext, exchange, tokenStore);

        return keycloakAuthenticate(exchange, securityContext, authenticator);
    }

}
