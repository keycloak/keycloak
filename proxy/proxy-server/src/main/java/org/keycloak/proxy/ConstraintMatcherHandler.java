/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.proxy;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ConstraintMatcherHandler implements HttpHandler {
    protected static Logger log = Logger.getLogger(ConstraintMatcherHandler.class);
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
        log.debugv("ConstraintMatcherHandler: {0}", exchange.getRelativePath());
        SingleConstraintMatch match = matcher.getSecurityInfo(exchange.getRelativePath(), exchange.getRequestMethod().toString());
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

        if (match.getRequiredRoles().isEmpty()
                && match.getEmptyRoleSemantic() == SecurityInfo.EmptyRoleSemantic.PERMIT_AND_INJECT_IF_AUTHENTICATED) {

            boolean successfulAuthenticatedMethodFound = isSuccessfulAuthenticatedMethodFound(exchange);

            if(successfulAuthenticatedMethodFound) {
                //in case of authenticated we go for injecting headers
                exchange.putAttachment(CONSTRAINT_KEY, match);
                securedHandler.handleRequest(exchange);
                return;
            } else {
                //in case of not authenticated we just show the resource
                unsecuredHandler.handleRequest(exchange);
                return;
            }
        }

        log.debug("found constraint");
        exchange.getSecurityContext().setAuthenticationRequired();
        exchange.putAttachment(CONSTRAINT_KEY, match);
        securedHandler.handleRequest(exchange);

    }

    private boolean isSuccessfulAuthenticatedMethodFound(HttpServerExchange exchange) {
        boolean successfulAuthenticatedMethodFound = false;
        List<AuthenticationMechanism> authenticationMechanisms = exchange.getSecurityContext().getAuthenticationMechanisms();

        for (AuthenticationMechanism authenticationMechanism : authenticationMechanisms) {
            AuthenticationMechanism.AuthenticationMechanismOutcome authenticationMechanismOutcome =
                    authenticationMechanism.authenticate(exchange, exchange.getSecurityContext());
            if(authenticationMechanismOutcome.equals(AuthenticationMechanism.AuthenticationMechanismOutcome.AUTHENTICATED)) {
                successfulAuthenticatedMethodFound = true;
            }
        }
        return successfulAuthenticatedMethodFound;
    }
}
