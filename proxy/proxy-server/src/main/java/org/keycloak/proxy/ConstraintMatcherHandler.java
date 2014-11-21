package org.keycloak.proxy;

import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.keycloak.KeycloakSecurityContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConstraintMatcherHandler implements HttpHandler {
    public static final AttachmentKey<SingleConstraintMatch> CONSTRAINT_KEY = AttachmentKey.create(SingleConstraintMatch.class);
    protected SecurityPathMatches matcher;
    protected HttpHandler securedHandler;
    protected HttpHandler unsecuredHandler;
    protected String errorPage;

    public ConstraintMatcherHandler(SecurityPathMatches matcher, HttpHandler securedHandler, HttpHandler unsecuredHandler, String errorPage) {
        this.matcher = matcher;
        this.securedHandler = securedHandler;
        this.unsecuredHandler = unsecuredHandler;
        this.errorPage = errorPage;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SingleConstraintMatch match = matcher.getSecurityInfo(exchange.getRelativePath(), exchange.getRequestMethod().toString()).getMergedConstraint();
        if (match == null || (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.PERMIT)) {
            unsecuredHandler.handleRequest(exchange);
            return;
        }

        if (match.getRequiredRoles().isEmpty() && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.DENY) {
            if (errorPage != null) {
                exchange.setRequestPath(errorPage);
                exchange.setRelativePath(errorPage);
                exchange.setResolvedPath(errorPage);
                unsecuredHandler.handleRequest(exchange);
            } else {
                exchange.setResponseCode(403);
                exchange.endExchange();
            }
            return;
        }
        exchange.getSecurityContext().setAuthenticationRequired();
        exchange.putAttachment(CONSTRAINT_KEY, match);
        securedHandler.handleRequest(exchange);

    }
}
