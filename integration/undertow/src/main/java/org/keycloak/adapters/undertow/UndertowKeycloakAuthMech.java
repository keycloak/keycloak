/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.adapters.undertow;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityNotification;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Sessions;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthChallenge;
import org.keycloak.adapters.AuthOutcome;
import org.keycloak.adapters.RequestAuthenticator;

/**
 * Abstract base class for a Keycloak-enabled Undertow AuthenticationMechanism.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public abstract class UndertowKeycloakAuthMech implements AuthenticationMechanism {
    public static final AttachmentKey<AuthChallenge> KEYCLOAK_CHALLENGE_ATTACHMENT_KEY = AttachmentKey.create(AuthChallenge.class);
    protected AdapterDeploymentContext deploymentContext;

    public UndertowKeycloakAuthMech(AdapterDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        AuthChallenge challenge = exchange.getAttachment(KEYCLOAK_CHALLENGE_ATTACHMENT_KEY);
        if (challenge != null) {
            UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
            if (challenge.challenge(facade)) {
                return new ChallengeResult(true, exchange.getResponseCode());
            }
        }
        return new ChallengeResult(false);
    }

    protected void registerNotifications(SecurityContext securityContext) {

        final NotificationReceiver logoutReceiver = new NotificationReceiver() {
            @Override
            public void handleNotification(SecurityNotification notification) {
                UndertowHttpFacade facade = new UndertowHttpFacade(notification.getExchange());
                if (notification.getEventType() != SecurityNotification.EventType.LOGGED_OUT) return;
                Session session = Sessions.getSession(notification.getExchange());
                if (session == null) return;
                KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
                if (account == null) return;
                session.removeAttribute(KeycloakUndertowAccount.class.getName());
                if (account.getKeycloakSecurityContext() != null) {
                    account.getKeycloakSecurityContext().logout(deploymentContext.getDeployment(facade.getRequest()));
                }
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

}