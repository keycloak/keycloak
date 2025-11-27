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

import java.util.List;
import java.util.Map;

import org.keycloak.OAuthErrorException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.JWTAuthorizationGrantValidationContext;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.JWTAuthorizationGrantContext;

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
        return DownscopeAssertionGrantEnforcerExecutorFactory.PROVIDER_ID;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty("claim-name")
        protected String claimName;

        @JsonProperty("allowed-values")
        protected List<String> allowedValues;

        public String getClaimName() {
            return claimName;
        }

        public void setClaimName(String claimName) {
            this.claimName = claimName;
        }

        public List<String> getAllowedValues() {
            return allowedValues;
        }

        public void setAllowedValues(List<String> allowedValues) {
            this.allowedValues = allowedValues;
        }
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case JWT_AUTHORIZATION_GRANT -> {
                JWTAuthorizationGrantContext jwtAuthnGrantContext = ((JWTAuthorizationGrantContext) context);
                JWTAuthorizationGrantValidationContext jwtContext = jwtAuthnGrantContext.getAuthorizationGrantContext();
                checkClaims(getAccessTokenAsMapFromAssertion(jwtContext.getAssertion()));
            }
        }
    }

    private Map<String, Object> getAccessTokenAsMapFromAssertion(String assertion) throws ClientPolicyException {
        try {
            return new  JWSInput(assertion).readJsonContent(new TypeReference<>() {});
        } catch (JWSInputException e) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Assertion contains an invalid access token");
        }
    }
    private void checkClaims(Map<String, Object> tokenMap) throws ClientPolicyException {
        String claimName = configuration.getClaimName();
        // Validate configuration
        if (claimName == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST,  "Invalid configuration");
        }

        List<String> allowedValues = configuration.getAllowedValues();

        // Extract claim value
        Object claimValue = tokenMap.get(claimName);
        if (claimValue == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Required claim '" + claimName + "' is missing from the token");
        }

        // If allowedValues is empty validate only if the claim exists
        if (allowedValues == null || allowedValues.isEmpty()) {
            return;
        }

        //allow only numbers or strings
        if (!isAllowedClaimType(claimValue)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Claim value is not allowed");
        }

        String stringValue = String.valueOf(claimValue);
        boolean matches = allowedValues.stream().anyMatch(allowed -> matchesAllowed(stringValue, allowed));
        if (!matches) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Claim '" + claimName + "' not allowed");
        }
    }

    private boolean isAllowedClaimType(Object claimValue) {
        return claimValue instanceof String || claimValue instanceof Number;
    }

    private boolean matchesAllowed(String actual, String allowed) {
        if (allowed.contains("*")) {
            String regex = allowed.replace("*", ".*");
            return actual.matches(regex);
        }
        return actual.equals(allowed);
    }

}
