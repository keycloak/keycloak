package org.keycloak.adapters.tomcat;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Pre-installed actions that must be authenticated
 * <p/>
 * Actions include:
 * <p/>
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsValve extends ValveBase {
    private static final Logger log = Logger.getLogger(AuthenticatedActionsValve.class);
    protected AdapterDeploymentContext deploymentContext;

    public AuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container) {
        this.deploymentContext = deploymentContext;
        if (next == null) throw new RuntimeException("Next valve is null!!!");
        setNext(next);
        setContainer(container);
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.debugv("AuthenticatedActionsValve.invoke {0}", request.getRequestURI());
        CatalinaHttpFacade facade = new CatalinaHttpFacade(request, response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment != null && deployment.isConfigured()) {
            AuthenticatedActionsHandler handler = new AuthenticatedActionsHandler(deployment, new CatalinaHttpFacade(request, response));
            if (handler.handledRequest()) {
                return;
            }

        }
        getNext().invoke(request, response);
    }
}
