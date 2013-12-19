package org.keycloak.adapters.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import org.jboss.logging.Logger;
import org.keycloak.SkeletonKeySession;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.representations.SkeletonKeyToken;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Set;

/**
 * Pre-installed actions that must be authenticated
 *
 * Actions include:
 *
 * CORS Origin Check and Response headers
 * K_QUERY_BEARER_TOKEN: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(AuthenticatedActionsHandler.class);
    protected AdapterConfig adapterConfig;
    protected HttpHandler next;

    protected AuthenticatedActionsHandler(AdapterConfig config, HttpHandler next) {
        this.adapterConfig = config;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        log.debugv("AuthenticatedActionsValve.invoke {0}", exchange.getRequestURI());
        SkeletonKeySession session = getSkeletonKeySession(exchange);
        if (corsRequest(exchange, session)) return;
        String requestUri = exchange.getRequestURI();
        if (requestUri.endsWith("K_QUERY_BEARER_TOKEN")) {
            queryBearerToken(exchange, session);
            return;
        }
        next.handleRequest(exchange);
    }

    public SkeletonKeySession getSkeletonKeySession(HttpServerExchange exchange) {
        SkeletonKeySession skSession = exchange.getAttachment(KeycloakAuthenticationMechanism.SKELETON_KEY_SESSION_ATTACHMENT_KEY);
        if (skSession != null) return skSession;
        return null;
    }

    protected void queryBearerToken(HttpServerExchange exchange, SkeletonKeySession session) throws IOException, ServletException {
        log.debugv("queryBearerToken {0}",exchange.getRequestURI());
        if (abortTokenResponse(exchange, session)) return;
        exchange.setResponseCode(200);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(session.getTokenString());
        exchange.endExchange();
    }

    protected boolean abortTokenResponse(HttpServerExchange exchange, SkeletonKeySession session) throws IOException {
        if (session == null) {
            log.debugv("session was null, sending back 401: {0}",exchange.getRequestURI());
            exchange.setResponseCode(200);
            exchange.endExchange();
            return true;
        }
        if (!adapterConfig.isExposeToken()) {
            exchange.setResponseCode(200);
            exchange.endExchange();
            return true;
        }
        if (!adapterConfig.isCors() && exchange.getRequestHeaders().getFirst(Headers.ORIGIN) != null) {
            exchange.setResponseCode(200);
            exchange.endExchange();
            return true;
        }
        return false;
    }

    protected boolean corsRequest(HttpServerExchange exchange, SkeletonKeySession session) throws IOException {
        if (!adapterConfig.isCors()) return false;
        log.debugv("CORS enabled + request.getRequestURI()");
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        log.debugv("Origin: {0} uri: {1}", origin, exchange.getRequestURI());
        if (session != null && origin != null) {
            SkeletonKeyToken token = session.getToken();
            Set<String> allowedOrigins = token.getAllowedOrigins();
            if (log.isDebugEnabled()) {
                for (String a : allowedOrigins) log.debug("   " + a);
            }
            if (allowedOrigins == null || (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin))) {
                if (allowedOrigins == null) {
                    log.debugv("allowedOrigins was null in token");
                }
                if (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin)) {
                    log.debugv("allowedOrigins did not contain origin");

                }
                exchange.setResponseCode(403);
                exchange.endExchange();
                return true;
            }
            log.debugv("returning origin: {0}", origin);
            exchange.setResponseCode(200);
            exchange.getResponseHeaders().put(PreflightCorsHandler.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            exchange.getResponseHeaders().put(PreflightCorsHandler.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            log.debugv("session or origin was null: {0}", exchange.getRequestURI());
        }
        return false;
    }
}
