package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jboss.logging.Logger;
import org.keycloak.adapters.config.ManagedResourceConfig;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PreflightCorsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(PreflightCorsHandler.class);
    protected ManagedResourceConfig config;
    protected HttpHandler next;

    public static final HttpString ACCESS_CONTROL_ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    public static final HttpString ACCESS_CONTROL_ALLOW_CREDENTIALS = new HttpString("Access-Control-Allow-Credentials");
    public static final HttpString ACCESS_CONTROL_ALLOW_METHODS = new HttpString("Access-Control-Allow-Methods");
    public static final HttpString ACCESS_CONTROL_ALLOW_HEADERS = new HttpString("Access-Control-Allow-Headers");
    public static final HttpString ACCESS_CONTROL_MAX_AGE = new HttpString("Access-Control-Max-Age");

    public static class Wrapper implements HandlerWrapper {
        protected ManagedResourceConfig config;

        public Wrapper(ManagedResourceConfig config) {
            this.config = config;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new PreflightCorsHandler(config, handler);
        }
    }

    protected PreflightCorsHandler(ManagedResourceConfig config, HttpHandler next) {
        this.config = config;
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
        exchange.setResponseCode(200);
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        String requestMethods = exchange.getRequestHeaders().getFirst("Access-Control-Request-Method");
        if (requestMethods != null) {
            if (config.getCorsAllowedMethods() != null) {
                requestMethods = config.getCorsAllowedMethods();
            }
            exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
        }
        String allowHeaders = exchange.getRequestHeaders().getFirst("Access-Control-Request-Headers");
        if (allowHeaders != null) {
            if (config.getCorsAllowedHeaders() != null) {
                allowHeaders = config.getCorsAllowedHeaders();
            }
            exchange.getResponseHeaders().put(ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
        }
        if (config.getCorsMaxAge() > -1) {
            exchange.getResponseHeaders().put(ACCESS_CONTROL_MAX_AGE, Integer.toString(config.getCorsMaxAge()));
        }
        exchange.endExchange();
    }
}
