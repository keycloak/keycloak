package org.keycloak.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.representations.IDToken;

import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConstraintAuthorizationHandler implements HttpHandler {
    public static final HttpString KEYCLOAK_SUBJECT = new HttpString("KEYCLOAK_SUBJECT");
    public static final HttpString KEYCLOAK_USERNAME = new HttpString("KEYCLOAK_USERNAME");
    public static final HttpString KEYCLOAK_EMAIL = new HttpString("KEYCLOAK_EMAIL");
    public static final HttpString KEYCLOAK_NAME = new HttpString("KEYCLOAK_NAME");
    public static final HttpString KEYCLOAK_ACCESS_TOKEN = new HttpString("KEYCLOAK_ACCESS_TOKEN");

    protected HttpHandler next;
    protected String errorPage;
    protected boolean sendAccessToken;

    public ConstraintAuthorizationHandler(HttpHandler next, String errorPage, boolean sendAccessToken) {
        this.next = next;
        this.errorPage = errorPage;
        this.sendAccessToken = sendAccessToken;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();
        SingleConstraintMatch match = exchange.getAttachment(ConstraintMatcherHandler.CONSTRAINT_KEY);
        if (match == null || (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)) {
            authenticatedRequest(account, exchange);
            return;
        }
        if (match != null) {
            for (String role : match.getRequiredRoles()) {
                if (account.getRoles().contains(role)) {
                    authenticatedRequest(account, exchange);
                    return;
                }
            }
        }
        if (errorPage != null) {
            exchange.setRequestPath(errorPage);
            exchange.setRelativePath(errorPage);
            exchange.setResolvedPath(errorPage);
            next.handleRequest(exchange);
            return;

        }
        exchange.setResponseCode(403);
        exchange.endExchange();

    }

    public void authenticatedRequest(KeycloakUndertowAccount account, HttpServerExchange exchange) throws Exception {
        if (account != null) {
            IDToken idToken = account.getKeycloakSecurityContext().getToken();
            if (idToken == null) return;
            if (idToken.getSubject() != null) {
                exchange.getRequestHeaders().put(KEYCLOAK_SUBJECT, idToken.getSubject());
            }
            if (idToken.getPreferredUsername() != null) {
                exchange.getRequestHeaders().put(KEYCLOAK_USERNAME, idToken.getPreferredUsername());
            }
            if (idToken.getEmail() != null) {
                exchange.getRequestHeaders().put(KEYCLOAK_EMAIL, idToken.getEmail());
            }
            if (idToken.getName() != null) {
                exchange.getRequestHeaders().put(KEYCLOAK_NAME, idToken.getName());
            }
            if (sendAccessToken) {
                exchange.getRequestHeaders().put(KEYCLOAK_ACCESS_TOKEN, account.getKeycloakSecurityContext().getTokenString());
            }
        }
        next.handleRequest(exchange);
    }
}
