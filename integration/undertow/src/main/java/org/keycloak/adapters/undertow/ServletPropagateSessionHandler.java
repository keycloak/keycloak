package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakAuthenticatedSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletPropagateSessionHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(ServletPropagateSessionHandler.class);

    protected HttpHandler next;

    protected ServletPropagateSessionHandler(HttpHandler next) {
        this.next = next;
    }

    public static final HandlerWrapper WRAPPER = new HandlerWrapper() {
        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletPropagateSessionHandler(handler);
        }
    };

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        log.debug("handleRequest");
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();
        if (account == null) {
            log.debug("Not logged in, nothing to propagate");
            next.handleRequest(exchange);
            return;
        }

        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        req.setAttribute(KeycloakAuthenticatedSession.class.getName(), account.getSession());

        HttpSession session = req.getSession(false);
        if (session == null) {
            next.handleRequest(exchange);
            return;
        }
        log.debug("propagating to HTTP Session");
        session.setAttribute(KeycloakAuthenticatedSession.class.getName(), account.getSession());
        next.handleRequest(exchange);
    }
}
