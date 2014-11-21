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

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.ConfidentialPortManager;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.enums.TokenStore;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 * @version $Revision: 1 $
 */
public class ServletKeycloakAuthMech extends AbstractUndertowKeycloakAuthMech {
    private static final Logger log = Logger.getLogger(ServletKeycloakAuthMech.class);

    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected ConfidentialPortManager portManager;

    public ServletKeycloakAuthMech(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement, NodesRegistrationManagement nodesRegistrationManagement, ConfidentialPortManager portManager) {
        super(deploymentContext, userSessionManagement);
        this.nodesRegistrationManagement = nodesRegistrationManagement;
        this.portManager = portManager;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = new UndertowHttpFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        nodesRegistrationManagement.tryRegister(deployment);

        RequestAuthenticator authenticator = createRequestAuthenticator(deployment, exchange, securityContext, facade);

        return keycloakAuthenticate(exchange, securityContext, authenticator);
    }

    protected RequestAuthenticator createRequestAuthenticator(KeycloakDeployment deployment, HttpServerExchange exchange, SecurityContext securityContext, UndertowHttpFacade facade) {

        int confidentialPort = getConfidentilPort(exchange);
        AdapterTokenStore tokenStore = getTokenStore(exchange, facade, deployment, securityContext);
        return new ServletRequestAuthenticator(facade, deployment,
                confidentialPort, securityContext, exchange, tokenStore);
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

    @Override
    protected AdapterTokenStore getTokenStore(HttpServerExchange exchange, HttpFacade facade, KeycloakDeployment deployment, SecurityContext securityContext) {
        if (deployment.getTokenStore() == TokenStore.SESSION) {
            return new ServletSessionTokenStore(exchange, deployment, sessionManagement, securityContext);
        } else {
            return new UndertowCookieTokenStore(facade, deployment, securityContext);
        }
    }

}
