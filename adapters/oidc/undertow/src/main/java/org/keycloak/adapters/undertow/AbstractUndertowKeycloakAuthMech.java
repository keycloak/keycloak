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
package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityNotification;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.enums.TokenStore;

/**
 * Abstract base class for a Keycloak-enabled Undertow AuthenticationMechanism.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public abstract class AbstractUndertowKeycloakAuthMech implements AuthenticationMechanism {
    public static final AttachmentKey<AuthChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(AuthChallenge.class);
    protected AdapterDeploymentContext deploymentContext;
    protected UndertowUserSessionManagement sessionManagement;
    protected String errorPage;

    public AbstractUndertowKeycloakAuthMech(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement, String errorPage) {
        this.deploymentContext = deploymentContext;
        this.sessionManagement = sessionManagement;
        this.errorPage = errorPage;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        AuthChallenge challenge = exchange.getAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY);
        if (challenge != null) {
            UndertowHttpFacade facade = createFacade(exchange);
            if (challenge.challenge(facade)) {
                return new ChallengeResult(true, exchange.getResponseCode());
            }
        }
        return new ChallengeResult(false);
    }

    public UndertowHttpFacade createFacade(HttpServerExchange exchange) {
        return new OIDCUndertowHttpFacade(exchange);
    }

    protected Integer servePage(final HttpServerExchange exchange, final String location) {
        sendRedirect(exchange, location);
        return StatusCodes.TEMPORARY_REDIRECT;
    }

    static void sendRedirect(final HttpServerExchange exchange, final String location) {
        // TODO - String concatenation to construct URLS is extremely error prone - switch to a URI which will better handle this.
        String loc = exchange.getRequestScheme() + "://" + exchange.getHostAndPort() + location;
        exchange.getResponseHeaders().put(Headers.LOCATION, loc);
    }



    protected void registerNotifications(final SecurityContext securityContext) {

        final NotificationReceiver logoutReceiver = new NotificationReceiver() {
            @Override
            public void handleNotification(SecurityNotification notification) {
                if (notification.getEventType() != SecurityNotification.EventType.LOGGED_OUT) return;

                HttpServerExchange exchange = notification.getExchange();
                UndertowHttpFacade facade = createFacade(exchange);
                KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
                KeycloakSecurityContext ksc = exchange.getAttachment(OIDCUndertowHttpFacade.KEYCLOAK_SECURITY_CONTEXT_KEY);
                if (!deployment.isBearerOnly() && ksc != null && ksc instanceof RefreshableKeycloakSecurityContext) {
                    ((RefreshableKeycloakSecurityContext) ksc).logout(deployment);
                }
                AdapterTokenStore tokenStore = getTokenStore(exchange, facade, deployment, securityContext);
                tokenStore.logout();
            }
        };

        securityContext.registerNotificationReceiver(logoutReceiver);
    }

    /**
     * Call this inside your authenticate method.
     */
    protected AuthenticationMechanismOutcome keycloakAuthenticate(HttpServerExchange exchange, SecurityContext securityContext, RequestAuthenticator authenticator) {
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            registerNotifications(securityContext);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, challenge);
        }

        if (outcome == AuthOutcome.FAILED) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    protected AdapterTokenStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, KeycloakDeployment deployment, SecurityContext securityContext) {
        if (deployment.getTokenStore() == TokenStore.SESSION) {
            return new UndertowSessionTokenStore(exchange, deployment, sessionManagement, securityContext);
        } else {
            return new UndertowCookieTokenStore(facade, deployment, securityContext);
        }
    }

}