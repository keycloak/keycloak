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
package org.keycloak.adapters.saml.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityNotification;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.keycloak.adapters.saml.SamlAuthenticator;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlDeploymentContext;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Abstract base class for a Keycloak-enabled Undertow AuthenticationMechanism.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public abstract class AbstractSamlAuthMech implements AuthenticationMechanism {

    private static final Logger LOG = Logger.getLogger(AbstractSamlAuthMech.class.getName());

    public static final AttachmentKey<AuthChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(AuthChallenge.class);
    protected SamlDeploymentContext deploymentContext;
    protected UndertowUserSessionManagement sessionManagement;
    protected String errorPage;

    public AbstractSamlAuthMech(SamlDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement, String errorPage) {
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

    protected Integer servePage(final HttpServerExchange exchange, final String location) {
        sendRedirect(exchange, location);
        return StatusCodes.TEMPORARY_REDIRECT;
    }

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*:");

    static void sendRedirect(final HttpServerExchange exchange, final String location) {
        if (location == null) {
            LOG.log(Level.WARNING, "Logout page not set.");
            exchange.setStatusCode(StatusCodes.NOT_FOUND);
            exchange.endExchange();
            return;
        }

        if (PROTOCOL_PATTERN.matcher(location).find()) {
            exchange.getResponseHeaders().put(Headers.LOCATION, location);
        } else {
            String loc = exchange.getRequestScheme() + "://" + exchange.getHostAndPort() + location;
            exchange.getResponseHeaders().put(Headers.LOCATION, loc);
        }
    }

    protected void registerNotifications(final SecurityContext securityContext) {

        final NotificationReceiver logoutReceiver = new NotificationReceiver() {
            @Override
            public void handleNotification(SecurityNotification notification) {
                if (notification.getEventType() != SecurityNotification.EventType.LOGGED_OUT)
                    return;

                HttpServerExchange exchange = notification.getExchange();
                UndertowHttpFacade facade = createFacade(exchange);
                SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
                SamlSessionStore sessionStore = getTokenStore(exchange, facade, deployment, securityContext);
                sessionStore.logoutAccount();
            }
        };

        securityContext.registerNotificationReceiver(logoutReceiver);
    }

    /**
     * Call this inside your authenticate method.
     */
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = createFacade(exchange);
        SamlDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }
        SamlSessionStore sessionStore = getTokenStore(exchange, facade, deployment, securityContext);
        SamlAuthenticator authenticator = null;
        if (exchange.getRequestPath().endsWith("/saml")) {
            authenticator = new UndertowSamlEndpoint(facade, deploymentContext.resolveDeployment(facade), sessionStore);
        } else {
            authenticator = new UndertowSamlAuthenticator(securityContext, facade, deploymentContext.resolveDeployment(facade), sessionStore);

        }

        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            registerNotifications(securityContext);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        }
        if (outcome == AuthOutcome.NOT_AUTHENTICATED) {
            // we are in passive mode and user is not authenticated, let app server to try another auth mechanism
            // See KEYCLOAK-2107, AbstractSamlAuthenticationHandler
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }
        if (outcome == AuthOutcome.LOGGED_OUT) {
            securityContext.logout();
            if (deployment.getLogoutPage() != null) {
                redirectLogout(deployment, exchange);
            }
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            exchange.putAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY, challenge);
            if (authenticator instanceof UndertowSamlEndpoint) {
                exchange.getSecurityContext().setAuthenticationRequired();
            }
        }

        if (outcome == AuthOutcome.FAILED) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
    }

    protected void redirectLogout(SamlDeployment deployment, HttpServerExchange exchange) {
        String page = deployment.getLogoutPage();
        sendRedirect(exchange, page);
        exchange.setStatusCode(StatusCodes.FOUND);
        exchange.endExchange();
    }

    protected UndertowHttpFacade createFacade(HttpServerExchange exchange) {
        return new UndertowHttpFacade(exchange);
    }

    protected abstract SamlSessionStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, SamlDeployment deployment, SecurityContext securityContext);
}