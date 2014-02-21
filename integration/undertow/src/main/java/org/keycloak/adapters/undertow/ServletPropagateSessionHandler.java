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
        log.info("handleRequest");
        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        KeycloakAuthenticatedSession skSession = (KeycloakAuthenticatedSession)req.getAttribute(KeycloakAuthenticatedSession.class.getName());
        if (skSession != null) {
            log.info("skSession is in request");
            next.handleRequest(exchange);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null) {
            log.info("http session was null, nothing to propagate");
            next.handleRequest(exchange);
            return;
        }
        skSession = (KeycloakAuthenticatedSession)session.getAttribute(KeycloakAuthenticatedSession.class.getName());
        if (skSession == null) {
            log.info("skSession not in http session, nothing to propagate");
            next.handleRequest(exchange);
            return;
        }
        log.info("propagating");
        req.setAttribute(KeycloakAuthenticatedSession.class.getName(), skSession);
        exchange.putAttachment(KeycloakAuthenticationMechanism.SKELETON_KEY_SESSION_ATTACHMENT_KEY, skSession);
        next.handleRequest(exchange);
    }
}
