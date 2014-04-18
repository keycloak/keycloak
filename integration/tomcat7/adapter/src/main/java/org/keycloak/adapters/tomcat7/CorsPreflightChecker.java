package org.keycloak.adapters.tomcat7;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * @author <a href="mailto:ungarida@gmail.com">Davide Ungari</a>
 * @version $Revision: 1 $
 */
public class CorsPreflightChecker {
    private static final Logger log = Logger.getLogger(""+CorsPreflightChecker.class);
    protected KeycloakDeployment deployment;

    public CorsPreflightChecker(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public boolean checkCorsPreflight(Request request, Response response) {
        log.finer("checkCorsPreflight " + request.getRequestURI());
        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
            log.finer("checkCorsPreflight: not options ");
            return false;

        }
        if (request.getHeader("Origin") == null) {
            log.finer("checkCorsPreflight: no origin header");
            return false;
        }
        log.finer("Preflight request returning");
        response.setStatus(HttpServletResponse.SC_OK);
        String origin = request.getHeader("Origin");
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Credentials", "true");
        String requestMethods = request.getHeader("Access-Control-Request-Method");
        if (requestMethods != null) {
            if (deployment.getCorsAllowedMethods() != null) {
                requestMethods = deployment.getCorsAllowedMethods();
            }
            response.setHeader("Access-Control-Allow-Methods", requestMethods);
        }
        String allowHeaders = request.getHeader("Access-Control-Request-Headers");
        if (allowHeaders != null) {
            if (deployment.getCorsAllowedHeaders() != null) {
                allowHeaders = deployment.getCorsAllowedHeaders();
            }
            response.setHeader("Access-Control-Allow-Headers", allowHeaders);
        }
        if (deployment.getCorsMaxAge() > -1) {
            response.setHeader("Access-Control-Max-Age", Integer.toString(deployment.getCorsMaxAge()));
        }
        return true;
    }

}
