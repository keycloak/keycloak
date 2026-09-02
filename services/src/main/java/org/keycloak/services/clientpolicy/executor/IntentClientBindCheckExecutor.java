/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.OAuthErrorException;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class IntentClientBindCheckExecutor implements ClientPolicyExecutorProvider<IntentClientBindCheckExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(IntentClientBindCheckExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;

    public IntentClientBindCheckExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return IntentClientBindCheckExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void setupConfiguration(IntentClientBindCheckExecutor.Configuration config) {
        this.configuration = Optional.ofNullable(config).orElse(createDefaultConfiguration());
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty("intent-client-bind-check-endpoint")
        protected String intentClientBindCheckEndpoint;

        @JsonProperty("intent-name")
        protected String intentName;

        public String getIntentClientBindCheckEndpoint() {
            return intentClientBindCheckEndpoint;
        }

        public void setIntentClientBindCheckEndpoint(String intentClientBindCheckEndpoint) {
            this.intentClientBindCheckEndpoint = intentClientBindCheckEndpoint;
        }

        public String getIntentName() {
            return intentName;
        }

        public void setIntentName(String intentName) {
            this.intentName = intentName;
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
        case AUTHORIZATION_REQUEST:
            checkIntentClientBind((AuthorizationRequestContext)context);
            break;
        default:
            return;
        }
    }

    private Configuration createDefaultConfiguration() {
        Configuration conf = new Configuration();
        return conf;
    }

    private void checkIntentClientBind(AuthorizationRequestContext context) throws ClientPolicyException {
        if (!isValidIntentClientBindCheckEndpoint()) {
            throw new ClientPolicyException(OAuthErrorException.SERVER_ERROR, "invalid Intent Client Bind Check Endpoint setting");
        }
        ClientModel client = session.getContext().getClient();
        String clientId = client.getClientId();
        String intentId = retrieveIntentId(context.getAuthorizationEndpointRequest());
        IntentBindCheckRequest request = new IntentBindCheckRequest();
        request.setClientId(clientId);
        request.setIntentId(intentId);
        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doPost(configuration.getIntentClientBindCheckEndpoint())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .json(request);
        IntentBindCheckResponse response = null;
        try {
            response = simpleHttp.asJson(IntentBindCheckResponse.class);
        } catch (IOException e) {
            logger.warnv("HTTP connection failure: {0}", e);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "checking intent bound with client failed");
        }
        if (!response.isBound.booleanValue()) {
            logger.tracev("Not Bound: intentName = {0}, intentId = {1}, clientId = {2}", configuration.getIntentName(), intentId, clientId);
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "The intent is not bound with the client");
        }
        logger.tracev("Bound: intentName = {0}, intentId = {1}, clientId = {2}", configuration.getIntentName(), intentId, clientId);
    }

    private String retrieveIntentId(AuthorizationEndpointRequest request) throws ClientPolicyException {
        String claims = request.getClaims();
        if (claims == null || claims.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no claim for an intent value in an authorization request");
        }

        String intentName = configuration.getIntentName();
        if (intentName == null || intentName.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid intent name setting");
        }

        ClaimsRepresentation claimsRep = null;

        try {
            claimsRep = JsonSerialization.readValue(claims, ClaimsRepresentation.class);
        } catch (IOException e) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid claim for an intent value");
        }

        if(!claimsRep.isPresent(intentName, ClaimsRepresentation.ClaimContext.ID_TOKEN) || claimsRep.isPresentAsNullClaim(intentName, ClaimsRepresentation.ClaimContext.ID_TOKEN)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no claim for an intent value for ID token");
        }

        ClaimsRepresentation.ClaimValue<String> claimValue = claimsRep.getClaimValue(intentName, ClaimsRepresentation.ClaimContext.ID_TOKEN, String.class);
        if (!claimValue.isEssential()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not specifying a claim for an intent as essential claim");
        }

        String value = claimValue.getValue();
        if (value == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid intent value");
        }

        return value;
    }

    private boolean isValidIntentClientBindCheckEndpoint() {
        String endpoint = configuration.getIntentClientBindCheckEndpoint();
        if (endpoint == null) return false;
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) return false;
        return true;
    }

    public static class IntentBindCheckRequest implements Serializable {

        @JsonProperty("intent_id")
        private String intentId;

        @JsonProperty("client_id")
        private String clientId;

        public String getIntentId() {
            return intentId;
        }

        public void setIntentId(String intentId) {
            this.intentId = intentId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }

    public static class IntentBindCheckResponse implements Serializable {

        @JsonProperty("is_bound")
        private Boolean isBound;

        public Boolean getIsBound() {
            return isBound;
        }

        public void setIsBound(Boolean isBound) {
            this.isBound = isBound;
        }
    }
}
