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

import io.undertow.security.api.NotificationReceiver;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.api.SecurityNotification;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import io.undertow.servlet.handlers.ServletRequestContext;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RequestAuthenticator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthMech extends UndertowKeycloakAuthMech {
    private static final Logger log = Logger.getLogger(ServletKeycloakAuthMech.class);

    protected UndertowUserSessionManagement userSessionManagement;
    protected ConfidentialPortManager portManager;

    public ServletKeycloakAuthMech(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement, ConfidentialPortManager portManager) {
        super(deploymentContext);
        this.userSessionManagement = userSessionManagement;
        this.portManager = portManager;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        RequestAuthenticator authenticator = createRequestAuthenticator(deployment, exchange, securityContext, facade);

        return keycloakAuthenticate(exchange, securityContext, authenticator);
    }

    @Override
    protected void registerNotifications(SecurityContext securityContext) {

        final NotificationReceiver logoutReceiver = new NotificationReceiver() {
            @Override
            public void handleNotification(SecurityNotification notification) {
                UndertowHttpFacade facade = new UndertowHttpFacade(notification.getExchange());
                if (notification.getEventType() != SecurityNotification.EventType.LOGGED_OUT) return;
                final ServletRequestContext servletRequestContext = notification.getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                HttpServletRequest req = (HttpServletRequest) servletRequestContext.getServletRequest();
                req.removeAttribute(KeycloakUndertowAccount.class.getName());
                req.removeAttribute(KeycloakSecurityContext.class.getName());
                HttpSession session = req.getSession(false);
                if (session == null) return;
                KeycloakUndertowAccount account = (KeycloakUndertowAccount)session.getAttribute(KeycloakUndertowAccount.class.getName());
                if (account == null) return;
                session.removeAttribute(KeycloakSecurityContext.class.getName());
                session.removeAttribute(KeycloakUndertowAccount.class.getName());
                if (account.getKeycloakSecurityContext() != null) {
                    account.getKeycloakSecurityContext().logout(deploymentContext.getDeployment(facade.getRequest()));
                }
            }
        };

        securityContext.registerNotificationReceiver(logoutReceiver);
    }



    protected RequestAuthenticator createRequestAuthenticator(KeycloakDeployment deployment, HttpServerExchange exchange, SecurityContext securityContext, UndertowHttpFacade facade) {

        int confidentialPort = getConfidentilPort(exchange);
        return new ServletRequestAuthenticator(facade, deployment,
                confidentialPort, securityContext, exchange, userSessionManagement);
    }

    protected int getConfidentilPort(HttpServerExchange exchange) {
        int confidentialPort = 8443;
        if (exchange.getRequestScheme().equalsIgnoreCase("HTTPS")) {
            confidentialPort = exchange.getHostPort();
        } else if (portManager != null) {
            confidentialPort = portManager.getConfidentialPort(exchange);
        }
        return confidentialPort;
    }

}
