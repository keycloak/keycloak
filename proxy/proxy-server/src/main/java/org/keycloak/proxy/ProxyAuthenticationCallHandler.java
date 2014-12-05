package org.keycloak.proxy;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProxyAuthenticationCallHandler implements HttpHandler {

    private final HttpHandler next;

    public ProxyAuthenticationCallHandler(final HttpHandler next) {
        this.next = next;
    }

    /**
     * Only allow the request through if successfully authenticated or if authentication is not required.
     *
     * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        SecurityContext context = exchange.getSecurityContext();
        if (context.authenticate()) {
            if(!exchange.isComplete()) {
                next.handleRequest(exchange);
            }
        } else {
            exchange.endExchange();
        }
    }
}
