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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureResponseTypeExecutor implements ClientPolicyExecutorProvider<SecureResponseTypeExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureResponseTypeExecutor.class);

    protected final KeycloakSession session;
    private Configuration configuration;

    public SecureResponseTypeExecutor(KeycloakSession session) {
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

        @JsonProperty("allow-token-response-type")
        protected Boolean allowTokenResponseType;

        public Boolean isAutoConfigure() {
            return autoConfigure;
        }

        public void setAutoConfigure(Boolean autoConfigure) {
            this.autoConfigure = autoConfigure;
        }

        public Boolean isAllowTokenResponseType() {
            return allowTokenResponseType;
        }

        public void setAllowTokenResponseType(Boolean allowTokenResponseType) {
            this.allowTokenResponseType = allowTokenResponseType;
        }
    }

    @Override
    public String getProviderId() {
        return SecureResponseTypeExecutorFactory.PROVIDER_ID;
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
            case AUTHORIZATION_REQUEST:
                AuthorizationRequestContext authorizationRequestContext = (AuthorizationRequestContext)context;
                executeOnAuthorizationRequest(authorizationRequestContext.getparsedResponseType(),
                    authorizationRequestContext.getAuthorizationEndpointRequest(),
                    authorizationRequestContext.getRedirectUri());
                break;
            default:
        }
        return;
    }

    // on Authorization Endpoint access for authorization request
    public void executeOnAuthorizationRequest(
            OIDCResponseType parsedResponseType,
            AuthorizationEndpointRequest request,
            String redirectUri) throws ClientPolicyException {
        logger.trace("Authz Endpoint - authz request");

        if (isHybridFlow(parsedResponseType)) {
            if (parsedResponseType.hasResponseType(OIDCResponseType.TOKEN)) {
                if (isAllowTokenResponseType()) {
                    logger.trace("Passed. response_type = code id_token token");
                    return;
                }
            } else {
                logger.trace("Passed. response_type = code id_token");
                return;
            }
        }

        if (request.getResponseMode() != null) {
            if (parsedResponseType.hasSingleResponseType(OIDCResponseType.CODE)) {
                if (OIDCResponseMode.JWT.name().equalsIgnoreCase(request.getResponseMode())) {
                    logger.trace("Passed. response_type = code and response_mode = jwt");
                    return;
                }
            }
        }

        logger.tracev("invalid response_type = {0}", parsedResponseType);
        throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid response_type");
    }

    private boolean isHybridFlow(OIDCResponseType parsedResponseType) {
        return parsedResponseType.hasResponseType(OIDCResponseType.CODE) && parsedResponseType.hasResponseType(OIDCResponseType.ID_TOKEN);
    }

    private boolean isAllowTokenResponseType() {
        return configuration != null && Optional.ofNullable(configuration.isAllowTokenResponseType()).orElse(Boolean.FALSE).booleanValue();
    }

    private void autoConfigure(ClientRepresentation rep) {
        if (isAutoConfigure()) {
            Map<String, String> attributes = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attributes.put(OIDCConfigAttributes.ID_TOKEN_AS_DETACHED_SIGNATURE, Boolean.TRUE.toString());
            rep.setAttributes(attributes);
        }
    }

    private boolean isAutoConfigure() {
        return configuration != null && Optional.ofNullable(configuration.isAutoConfigure()).orElse(Boolean.FALSE).booleanValue();
    }

    private void validate(ClientRepresentation rep) throws ClientPolicyException {
        if (!isIdTokenAsDetachedSignature(rep)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid client metadata: ID Token as detached signature in disabled");
        }
    }

    private boolean isIdTokenAsDetachedSignature(ClientRepresentation rep) {
        if (rep.getAttributes() == null) return false;
        return Boolean.valueOf(Optional.ofNullable(rep.getAttributes().get(OIDCConfigAttributes.ID_TOKEN_AS_DETACHED_SIGNATURE)).orElse(Boolean.FALSE.toString()));
    }

}
