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

package org.keycloak.testsuite.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaAuthenticationRequestSigningAlgorithmExecutor;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition;
import org.keycloak.services.clientpolicy.condition.ClientRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientScopesCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceHostsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesCondition;
import org.keycloak.services.clientpolicy.executor.FullScopeDisabledExecutor;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutor;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutor;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutor;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExeptionCondition;
import org.keycloak.util.JsonSerialization;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

public final class ClientPoliciesUtil {

    public static final ObjectMapper objectMapper = new ObjectMapper();

    // Client Profiles CRUD Operations

    public static class ClientProfilesBuilder {
        private final ClientProfilesRepresentation profilesRep;

        public ClientProfilesBuilder() {
            profilesRep = new ClientProfilesRepresentation();
            profilesRep.setProfiles(new ArrayList<>());
        }

        // Create client profile from existing representation
        public ClientProfilesBuilder(ClientProfilesRepresentation existingRep) {
            this.profilesRep = existingRep;
        }

        public ClientProfilesBuilder addProfile(ClientProfileRepresentation rep) {
            profilesRep.getProfiles().add(rep);
            return this;
        }

        public ClientProfilesRepresentation toRepresentation() {
            return profilesRep;
        }

        public String toString() {
            String profilesJson = null;
            try {
                profilesJson = objectMapper.writeValueAsString(profilesRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return profilesJson;
        }
    }

    public static class ClientProfileBuilder {

        private final ClientProfileRepresentation profileRep;

        public ClientProfileBuilder() {
            profileRep = new ClientProfileRepresentation();
        }

        public ClientProfileBuilder createProfile(String name, String description) {
            if (name != null) {
                profileRep.setName(name);
            }
            if (description != null) {
                profileRep.setDescription(description);
            }
            profileRep.setExecutors(new ArrayList<>());

            return this;
        }

        public ClientProfileBuilder addExecutor(String providerId, ClientPolicyExecutorConfigurationRepresentation config) throws Exception {
            if (config == null) {
                config = new ClientPolicyExecutorConfigurationRepresentation();
            }
            ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
            executor.setExecutorProviderId(providerId);
            executor.setConfiguration(JsonSerialization.mapper.readValue(JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class));
            profileRep.getExecutors().add(executor);
            return this;
        }

        public ClientProfileRepresentation toRepresentation() {
            return profileRep;
        }

        public String toString() {
            String profileJson = null;
            try {
                profileJson = objectMapper.writeValueAsString(profileRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return profileJson;
        }
    }

    // Client Profiles - Executor CRUD Operations

    public static HolderOfKeyEnforcerExecutor.Configuration createHolderOfKeyEnforceExecutorConfig(Boolean autoConfigure) {
        HolderOfKeyEnforcerExecutor.Configuration config = new HolderOfKeyEnforcerExecutor.Configuration();
        config.setAutoConfigure(autoConfigure);
        return config;
    }

    public static PKCEEnforcerExecutor.Configuration createPKCEEnforceExecutorConfig(Boolean autoConfigure) {
        PKCEEnforcerExecutor.Configuration config = new PKCEEnforcerExecutor.Configuration();
        config.setAutoConfigure(autoConfigure);
        return config;
    }

    public static FullScopeDisabledExecutor.Configuration createFullScopeDisabledExecutorConfig(Boolean autoConfigure) {
        FullScopeDisabledExecutor.Configuration config = new FullScopeDisabledExecutor.Configuration();
        config.setAutoConfigure(autoConfigure);
        return config;
    }

    public static SecureClientAuthenticatorExecutor.Configuration createSecureClientAuthenticatorExecutorConfig(List<String> allowedClientAuthenticators, String defaultClientAuthenticator) {
        SecureClientAuthenticatorExecutor.Configuration config = new SecureClientAuthenticatorExecutor.Configuration();
        config.setAllowedClientAuthenticators(allowedClientAuthenticators);
        config.setDefaultClientAuthenticator(defaultClientAuthenticator);
        return config;
    }

    public static SecureRequestObjectExecutor.Configuration createSecureRequestObjectExecutorConfig(Integer availablePeriod, Boolean verifyNbf) {
        SecureRequestObjectExecutor.Configuration config = new SecureRequestObjectExecutor.Configuration();
        if (availablePeriod != null) config.setAvailablePeriod(availablePeriod);
        if (verifyNbf != null) config.setVerifyNbf(verifyNbf);
        return config;
    }

    public static SecureResponseTypeExecutor.Configuration createSecureResponseTypeExecutor(Boolean autoConfigure, Boolean allowTokenResponseType) {
        SecureResponseTypeExecutor.Configuration config = new SecureResponseTypeExecutor.Configuration();
        if (autoConfigure != null) config.setAutoConfigure(autoConfigure);
        if (allowTokenResponseType != null) config.setAllowTokenResponseType(allowTokenResponseType);
        return config;
    }

    public static SecureSigningAlgorithmForSignedJwtExecutor.Configuration createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig(Boolean requireClientAssertion) {
        SecureSigningAlgorithmForSignedJwtExecutor.Configuration config = new SecureSigningAlgorithmForSignedJwtExecutor.Configuration();
        config.setRequireClientAssertion(requireClientAssertion);
        return config;
    }

    public static SecureSigningAlgorithmExecutor.Configuration createSecureSigningAlgorithmEnforceExecutorConfig(String defaultAlgorithm) {
        SecureSigningAlgorithmExecutor.Configuration config = new SecureSigningAlgorithmExecutor.Configuration();
        config.setDefaultAlgorithm(defaultAlgorithm);
        return config;
    }

    public static SecureCibaAuthenticationRequestSigningAlgorithmExecutor.Configuration createSecureCibaAuthenticationRequestSigningAlgorithmExecutorConfig(String defaultAlgorithm) {
        SecureCibaAuthenticationRequestSigningAlgorithmExecutor.Configuration config = new SecureCibaAuthenticationRequestSigningAlgorithmExecutor.Configuration();
        config.setDefaultAlgorithm(defaultAlgorithm);
        return config;
    }

    public static class ClientPoliciesBuilder {
        private final ClientPoliciesRepresentation policiesRep;

        public ClientPoliciesBuilder() {
            policiesRep = new ClientPoliciesRepresentation();
            policiesRep.setPolicies(new ArrayList<>());
        }

        public ClientPoliciesBuilder addPolicy(ClientPolicyRepresentation rep) {
            policiesRep.getPolicies().add(rep);
            return this;
        }

        public ClientPoliciesRepresentation toRepresentation() {
            return policiesRep;
        }

        public String toString() {
            String policiesJson = null;
            try {
                policiesJson = objectMapper.writeValueAsString(policiesRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return policiesJson;
        }
    }

    public static class ClientPolicyBuilder {

        private final ClientPolicyRepresentation policyRep;

        public ClientPolicyBuilder() {
            policyRep = new ClientPolicyRepresentation();
        }

        public ClientPolicyBuilder createPolicy(String name, String description, Boolean isEnabled) {
            policyRep.setName(name);
            if (description != null) {
                policyRep.setDescription(description);
            }
            if (isEnabled != null) {
                policyRep.setEnabled(isEnabled);
            } else {
                policyRep.setEnabled(Boolean.FALSE);
            }

            policyRep.setConditions(new ArrayList<>());
            policyRep.setProfiles(new ArrayList<>());

            return this;
        }

        public ClientPolicyBuilder addCondition(String providerId, ClientPolicyConditionConfigurationRepresentation config) throws Exception {
            ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
            condition.setConditionProviderId(providerId);
            condition.setConfiguration(JsonSerialization.mapper.readValue(JsonSerialization.mapper.writeValueAsBytes(config), JsonNode.class));
            policyRep.getConditions().add(condition);
            return this;
        }

        public ClientPolicyBuilder addProfile(String profileName) {
            policyRep.getProfiles().add(profileName);
            return this;
        }

        public ClientPolicyRepresentation toRepresentation() {
            return policyRep;
        }

        public String toString() {
            String policyJson = null;
            try {
                policyJson = objectMapper.writeValueAsString(policyRep);
            } catch (JsonProcessingException e) {
                fail();
            }
            return policyJson;
        }
    }

    // Client Policies - Condition CRUD Operations

    public static TestRaiseExeptionCondition.Configuration createTestRaiseExeptionConditionConfig() {
        return new TestRaiseExeptionCondition.Configuration();
    }

    public static ClientPolicyConditionConfigurationRepresentation createAnyClientConditionConfig() {
        return new ClientPolicyConditionConfigurationRepresentation();
    }

    public static ClientPolicyConditionConfigurationRepresentation createAnyClientConditionConfig(Boolean isNegativeLogic) {
        ClientPolicyConditionConfigurationRepresentation config = new ClientPolicyConditionConfigurationRepresentation();
        config.setNegativeLogic(isNegativeLogic);
        return config;
    }

    public static ClientAccessTypeCondition.Configuration createClientAccessTypeConditionConfig(List<String> types) {
        ClientAccessTypeCondition.Configuration config = new ClientAccessTypeCondition.Configuration();
        config.setType(types);
        return config;
    }

    public static ClientRolesCondition.Configuration createClientRolesConditionConfig(List<String> roles) {
        ClientRolesCondition.Configuration config = new ClientRolesCondition.Configuration();
        config.setRoles(roles);
        return config;
    }

    public static ClientScopesCondition.Configuration createClientScopesConditionConfig(String type, List<String> scopes) {
        ClientScopesCondition.Configuration config = new ClientScopesCondition.Configuration();
        config.setType(type);
        config.setScope(scopes);
        return config;
    }

    public static ClientUpdaterContextCondition.Configuration createClientUpdateContextConditionConfig(List<String> updateClientSource) {
        ClientUpdaterContextCondition.Configuration config = new ClientUpdaterContextCondition.Configuration();
        config.setUpdateClientSource(updateClientSource);
        return config;
    }

    public static ClientUpdaterSourceGroupsCondition.Configuration createClientUpdateSourceGroupsConditionConfig(List<String> groups) {
        ClientUpdaterSourceGroupsCondition.Configuration config = new ClientUpdaterSourceGroupsCondition.Configuration();
        config.setGroups(groups);
        return config;
    }

    public static ClientUpdaterSourceHostsCondition.Configuration createClientUpdateSourceHostsConditionConfig(List<String> trustedHosts) {
        ClientUpdaterSourceHostsCondition.Configuration config = new ClientUpdaterSourceHostsCondition.Configuration();
        config.setTrustedHosts(trustedHosts);
        return config;
    }

    public static ClientUpdaterSourceRolesCondition.Configuration createClientUpdateSourceRolesConditionConfig(List<String> roles) {
        ClientUpdaterSourceRolesCondition.Configuration config = new ClientUpdaterSourceRolesCondition.Configuration();
        config.setRoles(roles);
        return config;
    }
}
