/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.models.policy.conditions;

import java.util.List;
import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.policy.ResourcePolicyConditionProviderFactory;

public class IdentityProviderPolicyConditionFactory implements ResourcePolicyConditionProviderFactory<IdentityProviderPolicyConditionProvider> {

    public static final String ID = "identity-provider-condition";
    public static final String EXPECTED_ALIASES = "alias";

    @Override
    public IdentityProviderPolicyConditionProvider create(KeycloakSession session, Map<String, List<String>> config) {
        return new IdentityProviderPolicyConditionProvider(session, config.get(EXPECTED_ALIASES));
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}
