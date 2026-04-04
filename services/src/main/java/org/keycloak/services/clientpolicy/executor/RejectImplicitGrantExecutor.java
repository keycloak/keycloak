/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.par.clientpolicy.context.PushedAuthorizationRequestContext;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.OAuth2Constants.CODE;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class RejectImplicitGrantExecutor implements ClientPolicyExecutorProvider<RejectImplicitGrantExecutor.Configuration> {

    private final KeycloakSession session;
    private Configuration configuration;

    public RejectImplicitGrantExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("auto-configure")
        protected Boolean autoConfigure;

        public Boolean isAutoConfigure() {
            return autoConfigure;
        }

        public void setAutoConfigure(Boolean autoConfigure) {
            this.autoConfigure = autoConfigure;
        }
    }

    @Override
    public String getProviderId() {
        return RejectImplicitGrantExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientUpdateContext = (ClientCRUDContext)context;
                autoConfigure(clientUpdateContext.getProposedClientRepresentation());
                validate(clientUpdateContext.getProposedClientRepresentation());
                break;
            case PUSHED_AUTHORIZATION_REQUEST:
                PushedAuthorizationRequestContext pushedAuthorizationRequestContext = (PushedAuthorizationRequestContext)context;
                executeOnPushedAuthorizationRequest(pushedAuthorizationRequestContext.getRequest(),
                        pushedAuthorizationRequestContext.getRequestParameters());
                break;
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext)context;
                executeOnAuthorizationRequest(authorizationRequestContext.getparsedResponseType(),
                    authorizationRequestContext.getAuthorizationEndpointRequest(),
                    authorizationRequestContext.getRedirectUri());
                return;
            default:
                return;
        }
    }

    private void autoConfigure(ClientRepresentation rep) {
        if (configuration.isAutoConfigure())
            rep.setImplicitFlowEnabled(Boolean.FALSE);
    }

    private void validate(ClientRepresentation rep) throws ClientPolicyException {
        boolean isImplicitFlowEnabled = rep.isImplicitFlowEnabled().booleanValue();
        if (!isImplicitFlowEnabled) return;
        throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: implicit flow enabled");
    }

    private void executeOnAuthorizationRequest(
            OIDCResponseType parsedResponseType,
            AuthorizationEndpointRequest request,
            String redirectUri) throws ClientPolicyException {
        // Before client policies operation, Authorization Endpoint logic has already checked whether implicit/hybrid flow is activated for a client.
        // This method rejects implicit grant regardless of client setting for allowing implicit grant.
        if (parsedResponseType.isImplicitOrHybridFlow()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Implicit/Hybrid flow is prohibited.");
        }
    }

    private void executeOnPushedAuthorizationRequest(
            AuthorizationEndpointRequest request,
            MultivaluedMap<String, String> requestParameters) throws ClientPolicyException {
        if (request.getResponseType() == null || request.getResponseType().isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "No response type.");
        }
        if (!CODE.equals(request.getResponseType())) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Implicit/Hybrid flow is prohibited.");
        }
    }

}
