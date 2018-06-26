/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.camel.undertow;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.adapters.undertow.OIDCUndertowHttpFacade;
import org.keycloak.adapters.undertow.SessionManagementBridge;
import org.keycloak.adapters.undertow.UndertowCookieTokenStore;
import org.keycloak.adapters.undertow.UndertowRequestAuthenticator;
import org.keycloak.adapters.undertow.UndertowSessionTokenStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.enums.TokenStore;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.SecurityContextImpl;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionManager;
import io.undertow.util.AttachmentKey;
import io.undertow.util.StatusCodes;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.camel.Processor;
import org.apache.camel.component.undertow.UndertowConsumer;

/**
 *
 * @author hmlnarik
 */
public class UndertowKeycloakConsumer extends UndertowConsumer {

    private static final Logger LOG = Logger.getLogger(UndertowKeycloakConsumer.class.getName());

    public static final AttachmentKey<KeycloakPrincipal> KEYCLOAK_PRINCIPAL_KEY = AttachmentKey.create(KeycloakPrincipal.class);

    private static final IdentityManager IDENTITY_MANAGER = new IdentityManager() {
        @Override
        public Account verify(Account account) {
            return account;
        }

        @Override
        public Account verify(String id, Credential credential) {
            throw new IllegalStateException("Should never be called in Keycloak flow");
        }

        @Override
        public Account verify(Credential credential) {
            throw new IllegalStateException("Should never be called in Keycloak flow");
        }
    };

    protected SessionIdMapper idMapper = new InMemorySessionIdMapper();

    protected final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();

    private final UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();

    protected final AdapterDeploymentContext deploymentContext;

    protected final SessionManager sessionManager;

    protected final List<String> allowedRoles;

    private final int confidentialPort;

    private final Pattern skipPattern;

    public UndertowKeycloakConsumer(UndertowKeycloakEndpoint endpoint, Processor processor, 
      AdapterDeploymentContext deploymentContext, Pattern skipPattern, List<String> allowedRoles, int confidentialPort) {
        super(endpoint, processor);
        this.sessionManager = new InMemorySessionManager(endpoint.getEndpointUri());
        this.deploymentContext = deploymentContext;
        this.skipPattern = skipPattern;
        this.confidentialPort = confidentialPort;
        this.allowedRoles = allowedRoles == null ? Collections.<String>emptyList() : allowedRoles;
    }

    public int getConfidentialPort() {
        return confidentialPort;
    }

    @Override
    public void handleRequest(HttpServerExchange httpExchange) throws Exception {
        if (shouldSkip(httpExchange.getRequestPath())) {
            super.handleRequest(httpExchange);
            return;
        }

        //perform only non-blocking operation on exchange
        if (httpExchange.isInIoThread()) {
            httpExchange.dispatch(this);
            return;
        }

        OIDCUndertowHttpFacade facade = new OIDCUndertowHttpFacade(httpExchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);

        if (deployment == null || !deployment.isConfigured()) {
            httpExchange.setStatusCode(StatusCodes.FORBIDDEN);
            LOG.fine("deployment not configured");
            return;
        }

        LOG.fine("executing PreAuthActionsHandler");
        SessionManagementBridge bridge = new SessionManagementBridge(userSessionManagement, sessionManager);
        PreAuthActionsHandler preAuth = new PreAuthActionsHandler(bridge, deploymentContext, facade);
        if (preAuth.handleRequest()) return;

        SecurityContext securityContext = httpExchange.getSecurityContext();
        if (securityContext == null) {
            securityContext = new SecurityContextImpl(httpExchange, IDENTITY_MANAGER);
        }
        AdapterTokenStore tokenStore = getTokenStore(httpExchange, facade, deployment, securityContext);
        tokenStore.checkCurrentToken();

        LOG.fine("executing AuthenticatedActionsHandler");
        RequestAuthenticator authenticator = new UndertowRequestAuthenticator(facade, deployment, confidentialPort, securityContext, httpExchange, tokenStore);
        AuthOutcome outcome = authenticator.authenticate();

        if (outcome == AuthOutcome.AUTHENTICATED) {
            LOG.fine("AUTHENTICATED");
            if (httpExchange.isResponseComplete()) {
                return;
            }
            AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
            if (actions.handledRequest()) {
                return;
            } else {
                final Account authenticatedAccount = securityContext.getAuthenticatedAccount();
                if (authenticatedAccount instanceof KeycloakUndertowAccount) {
                    final KeycloakUndertowAccount kua = (KeycloakUndertowAccount) authenticatedAccount;
                    httpExchange.putAttachment(KEYCLOAK_PRINCIPAL_KEY, (KeycloakPrincipal) kua.getPrincipal());
                }

                Set<String> roles = Optional
                  .ofNullable(authenticatedAccount.getRoles())
                  .orElse((Set<String>) Collections.EMPTY_SET);

                LOG.log(Level.FINE, "Allowed roles: {0}, current roles: {1}", new Object[] {allowedRoles, roles});

                if (isRoleAllowed(roles, httpExchange)) {
                    super.handleRequest(httpExchange);
                } else {
                    httpExchange.setStatusCode(StatusCodes.FORBIDDEN);
                }

                return;
            }
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            LOG.fine("challenge");
            challenge.challenge(facade);
            return;
        }

        httpExchange.setStatusCode(StatusCodes.FORBIDDEN);
    }

    public boolean isRoleAllowed(Set<String> roles, HttpServerExchange httpExchange) throws Exception {
        for (String role : allowedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }

        return false;
    }

    protected AdapterTokenStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, KeycloakDeployment deployment, SecurityContext securityContext) {
        if (deployment.getTokenStore() == TokenStore.SESSION) {
            return new UndertowSessionTokenStore(exchange, deployment, userSessionManagement, securityContext);
        } else {
            return new UndertowCookieTokenStore(facade, deployment, securityContext);
        }
    }

    private boolean shouldSkip(String requestPath) {
        return skipPattern != null && skipPattern.matcher(requestPath).matches();
    }

}
