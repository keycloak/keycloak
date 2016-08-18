package org.keycloak.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.keycloak.constants.AdapterConstants;

/**
 * Dispatches requests for k_query_bearer_token through a worker thread (handler for this
 * resource performs blocking IO).
 */
public class TokenRequestPreHandler implements HttpHandler {

    private final HttpHandler next;

    public TokenRequestPreHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.getRequestURI().endsWith(AdapterConstants.K_QUERY_BEARER_TOKEN)) {
            exchange.dispatch(next);
        } else {
            next.handleRequest(exchange);
        }
    }
}
