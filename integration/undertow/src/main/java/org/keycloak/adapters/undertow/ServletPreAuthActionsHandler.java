package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
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
    protected AdapterDeploymentContext deploymentContext;

    public static class Wrapper implements HandlerWrapper {
        protected AdapterDeploymentContext deploymentContext;
        protected UndertowUserSessionManagement userSessionManagement;


        public Wrapper(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement) {
            this.deploymentContext = deploymentContext;
            this.userSessionManagement = userSessionManagement;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletPreAuthActionsHandler(deploymentContext, userSessionManagement, handler);
        }
    }

    protected ServletPreAuthActionsHandler(AdapterDeploymentContext deploymentContext,
                                           UndertowUserSessionManagement userSessionManagement,
                                           HttpHandler next) {
        this.next = next;
        this.deploymentContext = deploymentContext;
        this.userSessionManagement = userSessionManagement;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        SessionManagementBridge bridge = new SessionManagementBridge(userSessionManagement, servletRequestContext.getDeployment().getSessionManager());
        PreAuthActionsHandler handler = new PreAuthActionsHandler(bridge, deploymentContext, facade);
        if (handler.handleRequest()) return;
        next.handleRequest(exchange);
    }
}
