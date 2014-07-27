package org.keycloak.adapters.tomcat7;

import java.io.IOException;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.servlet.ServletException;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * Pre-installed actions that must be authenticated
 * <p/>
 * Actions include:
 * <p/>
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsValve extends ValveBase {
    private static final Logger log = Logger.getLogger(""+AuthenticatedActionsValve.class);
    protected AdapterDeploymentContext deploymentContext;

    public AuthenticatedActionsValve(AdapterDeploymentContext deploymentContext, Valve next, Container container, ObjectName controller) {
        this.deploymentContext = deploymentContext;
        if (next == null) throw new RuntimeException("WTF is next null?!");
        setNext(next);
        setContainer(container);
    }


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        log.finer("AuthenticatedActionsValve.invoke" + request.getRequestURI());
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