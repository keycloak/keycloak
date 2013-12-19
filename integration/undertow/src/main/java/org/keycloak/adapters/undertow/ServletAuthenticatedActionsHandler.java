package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.keycloak.SkeletonKeySession;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletAuthenticatedActionsHandler extends AuthenticatedActionsHandler {

    protected ServletAuthenticatedActionsHandler(AdapterConfig config, HttpHandler next) {
        super(config, next);
    }

    public static class Wrapper implements HandlerWrapper {
        protected AdapterConfig config;

        public Wrapper(AdapterConfig config) {
            this.config = config;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new ServletAuthenticatedActionsHandler(config, handler);
        }
    }

    @Override
    public SkeletonKeySession getSkeletonKeySession(HttpServerExchange exchange) {
        SkeletonKeySession skSession = super.getSkeletonKeySession(exchange);
        if (skSession != null) return skSession;

        final ServletRequestContext servletRequestContext = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
        HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
        HttpSession session = req.getSession(false);
        if (session == null) return null;
        return (SkeletonKeySession)session.getAttribute(SkeletonKeySession.class.getName());

    }
}
