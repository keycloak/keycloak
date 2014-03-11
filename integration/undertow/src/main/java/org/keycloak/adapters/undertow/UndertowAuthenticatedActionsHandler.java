package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.logging.Logger;
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
    protected KeycloakDeployment deployment;
    protected HttpHandler next;

    public static class Wrapper implements HandlerWrapper {
        protected KeycloakDeployment deployment;

        public Wrapper(KeycloakDeployment deployment) {
            this.deployment = deployment;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new UndertowAuthenticatedActionsHandler(deployment, handler);
        }
    }


    protected UndertowAuthenticatedActionsHandler(KeycloakDeployment deployment, HttpHandler next) {
        this.deployment = deployment;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, new UndertowHttpFacade(exchange));
        if (handler.handledRequest()) return;
        next.handleRequest(exchange);
    }
}
