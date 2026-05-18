/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.account;

import java.util.List;

import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.UseLightweightAccessTokenExecutorFactory;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;

/**
 * Re-runs {@link AccountRestServiceTest} with a realm-wide client policy that forces issued
 * access tokens to be lightweight, exercising the account REST surface against that shape of token.
 */
@KeycloakIntegrationTest
public class AccountRestServiceLightweightTokenTest extends AccountRestServiceTest {

    private static final String POLICY_NAME = "enable lightweight tokens";

    @Override
    @BeforeEach
    public void before() {
        super.before();
        configureLightweightTokens();
    }

    private void configureLightweightTokens() {
        ClientPolicyExecutorRepresentation executor = new ClientPolicyExecutorRepresentation();
        executor.setExecutorProviderId(UseLightweightAccessTokenExecutorFactory.PROVIDER_ID);
        // The executor factory requires a non-null configuration node, even when no fields are set.
        // The legacy ClientPoliciesUtil round-tripped a null config through Jackson which produced a
        // NullNode; here we set an empty object node for the same effect.
        executor.setConfiguration(JsonNodeFactory.instance.objectNode());

        ClientProfileRepresentation profile = new ClientProfileRepresentation();
        profile.setName(POLICY_NAME);
        profile.setDescription("Profile Lightweight Tokens");
        profile.setExecutors(List.of(executor));

        ClientProfilesRepresentation profiles = new ClientProfilesRepresentation();
        profiles.setProfiles(List.of(profile));

        ClientPolicyConditionRepresentation condition = new ClientPolicyConditionRepresentation();
        condition.setConditionProviderId(AnyClientConditionFactory.PROVIDER_ID);
        condition.setConfiguration(JsonNodeFactory.instance.objectNode());

        ClientPolicyRepresentation policy = new ClientPolicyRepresentation();
        policy.setName(POLICY_NAME);
        policy.setDescription("Policy Lightweight Tokens");
        policy.setEnabled(true);
        policy.setConditions(List.of(condition));
        policy.setProfiles(List.of(POLICY_NAME));

        ClientPoliciesRepresentation policies = new ClientPoliciesRepresentation();
        policies.setPolicies(List.of(policy));

        managedRealm.updateClientProfile(profiles.getProfiles());
        managedRealm.updateClientPolicy(policies.getPolicies());
    }
}
