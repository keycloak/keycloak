package org.keycloak.adapters.undertow;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterConstants;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.adapters.config.AdapterConfig;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Set;

/**
 * Pre-installed actions that must be authenticated
 *
 * Actions include:
 *
 * CORS Origin Check and Response headers
 * k_query_bearer_token: Get bearer token from server for Javascripts CORS requests
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AuthenticatedActionsHandler implements HttpHandler {
    private static final Logger log = Logger.getLogger(AuthenticatedActionsHandler.class);
    protected KeycloakDeployment deployment;
    protected HttpHandler next;

    public static class Wrapper implements HandlerWrapper {
        protected KeycloakDeployment deployment;

        public Wrapper(KeycloakDeployment deployment) {
            this.deployment = deployment;
        }

        @Override
        public HttpHandler wrap(HttpHandler handler) {
            return new AuthenticatedActionsHandler(deployment, handler);
        }
    }


    protected AuthenticatedActionsHandler(KeycloakDeployment deployment, HttpHandler next) {
        this.deployment = deployment;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        log.debugv("AuthenticatedActionsValve.invoke {0}", exchange.getRequestURI());
        KeycloakUndertowAccount account = getAccount(exchange);
        if (corsRequest(exchange, account)) return;
        String requestUri = exchange.getRequestURI();
        if (requestUri.endsWith(AdapterConstants.K_QUERY_BEARER_TOKEN)) {
            queryBearerToken(exchange, account);
            return;
        }
        next.handleRequest(exchange);
    }

    public KeycloakUndertowAccount getAccount(HttpServerExchange exchange) {
        return (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();
    }

    protected void queryBearerToken(HttpServerExchange exchange, KeycloakUndertowAccount account) throws IOException, ServletException {
        log.debugv("queryBearerToken {0}",exchange.getRequestURI());
        if (abortTokenResponse(exchange, account)) return;
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(account.getEncodedAccessToken());
        exchange.endExchange();
    }

    protected boolean abortTokenResponse(HttpServerExchange exchange, KeycloakUndertowAccount account) throws IOException {
        if (account == null) {
            log.debugv("Not logged in, sending back 401: {0}",exchange.getRequestURI());
            exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
            exchange.endExchange();
            return true;
        }
        if (!deployment.isExposeToken()) {
            exchange.setResponseCode(StatusCodes.OK);
            exchange.endExchange();
            return true;
        }
        // Don't allow a CORS request if we're not validating CORS requests.
        if (!deployment.isCors() && exchange.getRequestHeaders().getFirst(Headers.ORIGIN) != null) {
            exchange.setResponseCode(StatusCodes.OK);
            exchange.endExchange();
            return true;
        }
        return false;
    }

    protected boolean corsRequest(HttpServerExchange exchange, KeycloakUndertowAccount account) throws IOException {
        if (!deployment.isCors()) return false;
        log.debugv("CORS enabled + request.getRequestURI()");
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        log.debugv("Origin: {0} uri: {1}", origin, exchange.getRequestURI());
        if (account != null && origin != null) {
            AccessToken token = account.getAccessToken();
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
                exchange.setResponseCode(StatusCodes.FORBIDDEN);
                exchange.endExchange();
                return true;
            }
            log.debugv("returning origin: {0}", origin);
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(PreflightCorsHandler.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            exchange.getResponseHeaders().put(PreflightCorsHandler.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        } else {
            log.debugv("cors validation not needed as we're not a secure session or origin header was null: {0}", exchange.getRequestURI());
        }
        return false;
    }
}
