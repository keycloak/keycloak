package org.keycloak.adapters.as7;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CorsPreflightChecker {
    private static final Logger log = Logger.getLogger(CorsPreflightChecker.class);
    protected KeycloakDeployment deployment;

    public CorsPreflightChecker(KeycloakDeployment deployment) {
        this.deployment = deployment;
    }

    public boolean checkCorsPreflight(Request request, Response response) {
        log.debugv("checkCorsPreflight {0}", request.getRequestURI());
        if (!request.getMethod().equalsIgnoreCase("OPTIONS")) {
            log.debug("checkCorsPreflight: not options ");
            return false;

        }
        if (request.getHeader("Origin") == null) {
            log.debug("checkCorsPreflight: no origin header");
            return false;
        }
        log.debug("Preflight request returning");
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
