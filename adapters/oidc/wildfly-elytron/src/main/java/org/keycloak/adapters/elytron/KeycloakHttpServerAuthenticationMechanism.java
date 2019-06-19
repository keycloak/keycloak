/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.adapters.elytron;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RequestAuthenticator;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerRequest;
import org.wildfly.security.http.Scope;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
class KeycloakHttpServerAuthenticationMechanism implements HttpServerAuthenticationMechanism {

    static Logger LOGGER = Logger.getLogger(KeycloakHttpServerAuthenticationMechanism.class);
    static final String NAME = "KEYCLOAK";

    private final Map<String, ?> properties;
    private final CallbackHandler callbackHandler;
    private final AdapterDeploymentContext deploymentContext;

    public KeycloakHttpServerAuthenticationMechanism(Map<String, ?> properties, CallbackHandler callbackHandler, AdapterDeploymentContext deploymentContext) {
        this.properties = properties;
        this.callbackHandler = callbackHandler;
        this.deploymentContext = deploymentContext;
    }

    @Override
    public String getMechanismName() {
        return NAME;
    }

    @Override
    public void evaluateRequest(HttpServerRequest request) throws HttpAuthenticationException {
        LOGGER.debugf("Evaluating request for path [%s]", request.getRequestURI());
        AdapterDeploymentContext deploymentContext = getDeploymentContext(request);

        if (deploymentContext == null) {
            LOGGER.debugf("Ignoring request for path [%s] from mechanism [%s]. No deployment context found.", request.getRequestURI(), getMechanismName());
            request.noAuthenticationInProgress();
            return;
        }

        ElytronHttpFacade httpFacade = new ElytronHttpFacade(request, deploymentContext, callbackHandler);
        KeycloakDeployment deployment = httpFacade.getDeployment();

        if (!deployment.isConfigured()) {
            request.noAuthenticationInProgress();
            return;
        }

        RequestAuthenticator authenticator = createRequestAuthenticator(request, httpFacade, deployment);

        httpFacade.getTokenStore().checkCurrentToken();

        if (preActions(httpFacade, deploymentContext)) {
            LOGGER.debugf("Pre-actions has aborted the evaluation of [%s]", request.getRequestURI());
            httpFacade.authenticationInProgress();
            return;
        }

        AuthOutcome outcome = authenticator.authenticate();

        if (AuthOutcome.AUTHENTICATED.equals(outcome)) {
            if (new AuthenticatedActionsHandler(deployment, httpFacade).handledRequest()) {
                httpFacade.authenticationInProgress();
            } else {
                httpFacade.authenticationComplete();
            }
            return;
        }

        AuthChallenge challenge = authenticator.getChallenge();

        if (challenge != null) {
            httpFacade.noAuthenticationInProgress(challenge);
            return;
        }

        if (AuthOutcome.FAILED.equals(outcome)) {
            httpFacade.getResponse().setStatus(403);
            httpFacade.authenticationFailed();
            return;
        }

        httpFacade.noAuthenticationInProgress();
    }

    private ElytronRequestAuthenticator createRequestAuthenticator(HttpServerRequest request, ElytronHttpFacade httpFacade, KeycloakDeployment deployment) {
        return new ElytronRequestAuthenticator(this.callbackHandler, httpFacade, deployment, getConfidentialPort(request));
    }

    private AdapterDeploymentContext getDeploymentContext(HttpServerRequest request) {
        if (this.deploymentContext == null) {
            return (AdapterDeploymentContext) request.getScope(Scope.APPLICATION).getAttachment(KeycloakConfigurationServletListener.ADAPTER_DEPLOYMENT_CONTEXT_ATTRIBUTE_ELYTRON);
        }

        return this.deploymentContext;
    }

    private boolean preActions(ElytronHttpFacade httpFacade, AdapterDeploymentContext deploymentContext) {
        NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();

        nodesRegistrationManagement.tryRegister(httpFacade.getDeployment());

        PreAuthActionsHandler preActions = new PreAuthActionsHandler(UserSessionManagement.class.cast(httpFacade.getTokenStore()), deploymentContext, httpFacade);

        return preActions.handleRequest();
    }

    // TODO: obtain confidential port from Elytron
    private int getConfidentialPort(HttpServerRequest request) {
        return 8443;
    }
}
