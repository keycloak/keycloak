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
import org.apache.camel.component.undertow.spi.UndertowSecurityProvider;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.undertow.KeycloakUndertowAccount;
import org.keycloak.adapters.undertow.OIDCUndertowHttpFacade;
import org.keycloak.adapters.undertow.SessionManagementBridge;
import org.keycloak.adapters.undertow.UndertowCookieTokenStore;
import org.keycloak.adapters.undertow.UndertowRequestAuthenticator;
import org.keycloak.adapters.undertow.UndertowSessionTokenStore;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import org.keycloak.enums.TokenStore;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class UndertowKeycloakProvider implements UndertowSecurityProvider {

    public static final AttachmentKey<KeycloakPrincipal> KEYCLOAK_PRINCIPAL_KEY = AttachmentKey.create(KeycloakPrincipal.class);
    private static final Logger LOG = Logger.getLogger(UndertowKeycloakProvider.class.getName());


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

    private AdapterDeploymentContext deploymentContext;
    private SessionManager sessionManager;
    private UndertowKeycloakProviderConfiguration configuration;
    private AdapterConfig adapterConfig;
    private final UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();

    @Override
    public void addHeader(BiConsumer<String, Object> consumer, HttpServerExchange httpExchange) throws Exception {
        KeycloakPrincipal principal = httpExchange.getAttachment(KEYCLOAK_PRINCIPAL_KEY);
        LOG.log(Level.FINE, "principal: {0}", principal);
        if (principal != null) {
            consumer.accept(KeycloakPrincipal.class.getName(), principal);
        }
    }

    @Override
    public int authenticate(HttpServerExchange httpExchange, List<String> allowedRoles) throws Exception {
        if (shouldSkip(httpExchange.getRequestPath())) {
            return StatusCodes.OK;
        }

        OIDCUndertowHttpFacade facade = new OIDCUndertowHttpFacade(httpExchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);

        if (deployment == null || !deployment.isConfigured()) {
            httpExchange.setStatusCode(StatusCodes.FORBIDDEN);
            LOG.fine("deployment not configured");
            //request is forbidden
            return StatusCodes.FORBIDDEN;
        }

        LOG.fine("executing PreAuthActionsHandler");
        SessionManagementBridge bridge = new SessionManagementBridge(userSessionManagement, sessionManager);
        PreAuthActionsHandler preAuth = new PreAuthActionsHandler(bridge, deploymentContext, facade);
        if (preAuth.handleRequest()) return StatusCodes.OK;

        SecurityContext securityContext = httpExchange.getSecurityContext();
        if (securityContext == null) {
            securityContext = new SecurityContextImpl(httpExchange, IDENTITY_MANAGER);
        }
        AdapterTokenStore tokenStore = getTokenStore(httpExchange, facade, deployment, securityContext);
        tokenStore.checkCurrentToken();

        LOG.fine("executing AuthenticatedActionsHandler");
        RequestAuthenticator authenticator = new UndertowRequestAuthenticator(facade, deployment, configuration.getConfidentialPort(), securityContext, httpExchange, tokenStore);
        AuthOutcome outcome = authenticator.authenticate();

        if (outcome == AuthOutcome.AUTHENTICATED) {
            LOG.fine("AUTHENTICATED");
            if (httpExchange.isResponseComplete()) {
                return StatusCodes.OK;
            }
            AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
            if (actions.handledRequest()) {
                return StatusCodes.OK;
            } else {
                final Account authenticatedAccount = securityContext.getAuthenticatedAccount();
                if (authenticatedAccount instanceof KeycloakUndertowAccount) {
                    final KeycloakUndertowAccount kua = (KeycloakUndertowAccount) authenticatedAccount;
                    httpExchange.putAttachment(KEYCLOAK_PRINCIPAL_KEY, (KeycloakPrincipal) kua.getPrincipal());
                }

                Set<String> roles = Optional
                        .ofNullable(authenticatedAccount.getRoles())
                        .orElse((Set<String>) Collections.EMPTY_SET);

                LOG.log(Level.FINE, "Allowed roles: {0}, current roles: {1}", new Object[]{allowedRoles, roles});

                if (isRoleAllowed(roles, allowedRoles)) {
                    return StatusCodes.OK;
                }

                return StatusCodes.FORBIDDEN;
            }
        }

        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            LOG.fine("challenge");
            challenge.challenge(facade);
            return challenge.getResponseCode();
        }

        return StatusCodes.FORBIDDEN;
    }

    @Override
    public boolean acceptConfiguration(Object configuration, String endpointUri) throws Exception {
        if(configuration instanceof UndertowKeycloakProviderConfiguration) {
            this.configuration = (UndertowKeycloakProviderConfiguration) configuration;
            this.sessionManager = new InMemorySessionManager(endpointUri);
            this.deploymentContext = getDeploymentContext();
            return true;
        }
        return false;
    }


    private AdapterDeploymentContext getDeploymentContext() {
        if (configuration != null) {
            LOG.log(Level.INFO, "Using {0} to resolve Keycloak configuration on a per-request basis.", configuration.getClass());
            return new AdapterDeploymentContext(configuration);
        } else if (adapterConfig != null) {
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(adapterConfig);
            return new AdapterDeploymentContext(kd);
        }

        LOG.warning("Adapter is unconfigured, Keycloak will deny every request");
        return new AdapterDeploymentContext();
    }

    private Pattern getSkipPatternAsPattern() {
        return configuration.getSkipPattern() == null
                ? null
                : Pattern.compile(configuration.getSkipPattern(), Pattern.DOTALL);
    }

    private boolean shouldSkip(String requestPath) {
        return getSkipPatternAsPattern() != null && getSkipPatternAsPattern().matcher(requestPath).matches();
    }

    private AdapterTokenStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, KeycloakDeployment deployment, SecurityContext securityContext) {
        if (deployment.getTokenStore() == TokenStore.SESSION) {
            return new UndertowSessionTokenStore(exchange, deployment, userSessionManagement, securityContext);
        } else {
            return new UndertowCookieTokenStore(facade, deployment, securityContext);
        }
    }

    private boolean isRoleAllowed(Set<String> roles, List<String> allowedRoles) throws Exception {
        for (String role : allowedRoles) {
            if (roles.contains(role)) {
                return true;
            }
        }

        return false;
    }
}
