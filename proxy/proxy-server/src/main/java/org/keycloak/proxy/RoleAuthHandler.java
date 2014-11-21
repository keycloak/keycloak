package org.keycloak.proxy;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;

import java.util.Collection;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleAuthHandler implements HttpHandler {

    protected Collection<String> roles;
    protected HttpHandler next;

    public RoleAuthHandler(Collection<String> roles, HttpHandler next) {
        this.roles = roles;
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        KeycloakUndertowAccount account = (KeycloakUndertowAccount)exchange.getSecurityContext().getAuthenticatedAccount();
        SingleConstraintMatch match = exchange.getAttachment(ConstraintMatcherHandler.CONSTRAINT_KEY);
        if (match == null || (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.AUTHENTICATE)) {
            next.handleRequest(exchange);
            return;
        }
        if (match != null) {
            for (String role : match.getRequiredRoles()) {
                if (account.getRoles().contains(role)) {
                    next.handleRequest(exchange);
                    return;
                }
            }
        }
        exchange.setResponseCode(403);
        exchange.endExchange();

    }
}
