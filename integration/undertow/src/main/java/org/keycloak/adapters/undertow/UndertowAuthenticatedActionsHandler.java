package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * Bridge for authenticated Keycloak adapter actions
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowAuthenticatedActionsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(UndertowAuthenticatedActionsHandler.class);
    protected AdapterDeploymentContext deploymentContext;
    protected HttpHandler next;

    public static class Wrapper implements HandlerWrapper {
        protected AdapterDeploymentContext deploymentContext;

        public Wrapper(AdapterDeploymentContext deploymentContext) {
            this.deploymentContext = deploymentContext;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new UndertowAuthenticatedActionsHandler(deploymentContext, handler);
        }
    }


    protected UndertowAuthenticatedActionsHandler(AdapterDeploymentContext deploymentContext, HttpHandler next) {
        this.deploymentContext = deploymentContext;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment != null && deployment.isConfigured()) {
            AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, facade);
            if (handler.handledRequest()) return;
        }
        next.handleRequest(exchange);
    }
}
