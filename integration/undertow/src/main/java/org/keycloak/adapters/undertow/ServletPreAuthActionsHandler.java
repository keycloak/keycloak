package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletPreAuthActionsHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(ServletPreAuthActionsHandler.class);
    protected HttpHandler next;
    protected UndertowUserSessionManagement userSessionManagement;
    protected KeycloakDeployment deployment;

    public static class Wrapper implements HandlerWrapper {
        protected KeycloakDeployment deployment;
        protected UndertowUserSessionManagement userSessionManagement;


        public Wrapper(KeycloakDeployment deployment, UndertowUserSessionManagement userSessionManagement) {
            this.deployment = deployment;
            this.userSessionManagement = userSessionManagement;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletPreAuthActionsHandler(deployment, userSessionManagement, handler);
        }
    }

    protected ServletPreAuthActionsHandler(KeycloakDeployment deployment,
                                           UndertowUserSessionManagement userSessionManagement,
                                           HttpHandler next) {
        this.next = next;
        this.deployment = deployment;
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        SessionManagementBridge bridge = new SessionManagementBridge(userSessionManagement, servletRequestContext.getDeployment().getSessionManager());
        PreAuthActionsHandler handler = new PreAuthActionsHandler(bridge, deployment, new UndertowHttpFacade(exchange));
        if (handler.handleRequest()) return;
        next.handleRequest(exchange);
    }
}
