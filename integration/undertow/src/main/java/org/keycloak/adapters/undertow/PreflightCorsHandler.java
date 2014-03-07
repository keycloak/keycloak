package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PreflightCorsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(PreflightCorsHandler.class);
    protected KeycloakDeployment deployment;
    protected HttpHandler next;

    public static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    public static final HttpString ACCESS_CONTROL_ALLOW_CREDENTIALS = new HttpString("Access-Control-Allow-Credentials");
    public static final HttpString ACCESS_CONTROL_ALLOW_METHODS = new HttpString("Access-Control-Allow-Methods");
    public static final HttpString ACCESS_CONTROL_ALLOW_HEADERS = new HttpString("Access-Control-Allow-Headers");
    public static final HttpString ACCESS_CONTROL_MAX_AGE = new HttpString("Access-Control-Max-Age");

    public static class Wrapper implements HandlerWrapper {
        protected KeycloakDeployment deployment;

        public Wrapper(KeycloakDeployment deployment) {
            this.deployment = deployment;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new PreflightCorsHandler(deployment, handler);
        }
    }

    protected PreflightCorsHandler(KeycloakDeployment deployment, HttpHandler next) {
        this.deployment = deployment;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        log.debugv("checkCorsPreflight {0}", exchange.getRequestURI());
        if (!exchange.getRequestMethod().toString().equalsIgnoreCase("OPTIONS")) {
            log.debug("checkCorsPreflight: not options ");
            next.handleRequest(exchange);
            return;
        }
        if (exchange.getRequestHeaders().getFirst("Origin") == null) {
            log.debug("checkCorsPreflight: no origin header");
            next.handleRequest(exchange);
            return;
        }
        log.debug("Preflight request returning");
        exchange.setResponseCode(StatusCodes.OK);
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        String requestMethods = exchange.getRequestHeaders().getFirst("Access-Control-Request-Method");
        if (requestMethods != null) {
            if (deployment.getCorsAllowedMethods() != null) {
                requestMethods = deployment.getCorsAllowedMethods();
            }
            exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
        }
        String allowHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");
        if (allowHeaders != null) {
            if (deployment.getCorsAllowedHeaders() != null) {
                allowHeaders = deployment.getCorsAllowedHeaders();
            }
            exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
        }
        if (deployment.getCorsMaxAge() > -1) {
            exchange.getResponseHeaders().put(ACCESS_CONTROL_MAX_AGE, Integer.toString(deployment.getCorsMaxAge()));
        }
        exchange.endExchange();
    }
}
