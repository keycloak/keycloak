/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.JWTAuthorizationGrantContext;
import org.keycloak.services.clientpolicy.context.TokenExchangeRequestContext;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

public class JWTClaimEnforcerExecutor implements ClientPolicyExecutorProvider<JWTClaimEnforcerExecutor.Configuration> {

    private final KeycloakSession session;
    private Configuration configuration;

    public JWTClaimEnforcerExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(JWTClaimEnforcerExecutor.Configuration config) {
        this.configuration = config;
    }

    @Override
    public String getProviderId() {
        return JWTClaimEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty("claim-name")
        protected String claimName;

        @JsonProperty("allowed-value")
        protected String allowedValue;

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

        public String getAllowedValue() {
            return allowedValue;
        }

        public void setAllowedValue(String allowedValue) {
            this.allowedValue = allowedValue;
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case JWT_AUTHORIZATION_GRANT -> {
                JWTAuthorizationGrantContext jwtAuthnGrantContext = ((JWTAuthorizationGrantContext) context);
                JWTAuthorizationGrantValidationContext jwtContext = jwtAuthnGrantContext.getAuthorizationGrantContext();
                checkClaims(getAccessTokenMapFromJWTString(jwtContext.getAssertion()));
            }
            case TOKEN_EXCHANGE_REQUEST -> {
                TokenExchangeContext tokenExchangeContext = ((TokenExchangeRequestContext) context).getTokenExchangeContext();
                if (!OAuth2Constants.ACCESS_TOKEN_TYPE.equals(tokenExchangeContext.getParams().getSubjectTokenType())) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Parameter 'subject_token' should be access_token for the executor");
                }
                checkClaims(getAccessTokenMapFromJWTString(tokenExchangeContext.getParams().getSubjectToken()));
            }
        }
    }

    private  Map<String, Object> getAccessTokenMapFromJWTString(String jwt) throws ClientPolicyException {
        try {
            return new  JWSInput(jwt).readJsonContent(new TypeReference<>() {});
        } catch (JWSInputException e) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "JWT is not valid");
        }
    }

    private void checkClaims(Map<String, Object> tokenMap) throws ClientPolicyException {
        String claimName = configuration.getClaimName();
        // Validate configuration
        if (claimName == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST,  "Invalid configuration");
        }

        String allowedValue = configuration.getAllowedValue();

        // Extract claim value
        Object claimValue = tokenMap.get(claimName);
        if (claimValue == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Required claim '" + claimName + "' is missing from the token");
        }

        // If allowedValue is empty validate only if the claim exists
        if (StringUtil.isBlank(allowedValue)) {
            return;
        }

        //allow only numbers or strings
        if (!isAllowedClaimType(claimValue)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Value type for claim '" + claimName + "' not allowed");
        }

        String stringValue = String.valueOf(claimValue);

        if (!stringValue.matches(allowedValue)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Value for claim '" + claimName + "' not allowed");
        }
    }

    private boolean isAllowedClaimType(Object claimValue) {
        return claimValue instanceof String || claimValue instanceof Number;
    }
}
