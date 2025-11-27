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

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class JWTClaimEnforcerExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "jwt-claim-enforcer";

    public static final String CLAIM_NAME = "claim-name";

    public static final String ALLOWED_VALUES = "allowed-values";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new JWTClaimEnforcerExecutor(session);
    }

    private static final ProviderConfigProperty CLAIM_NAME_PROPERTY = new ProviderConfigProperty(
            CLAIM_NAME,
            "Claim Name",
            "The name of the JWT claim to enforce. This claim must be present in the token for validation.",
            ProviderConfigProperty.STRING_TYPE,
            null
    );

    private static final ProviderConfigProperty ALLOWED_VALUES_PROPERTY = new ProviderConfigProperty(
            ALLOWED_VALUES,
            "Allowed Values",
            "List of allowed values that the JWT claim can contain. You can use wildcards '*' to match any sequence of characters. " +
                    "If left empty, only the presence of the claim is enforced.",
            ProviderConfigProperty.MULTIVALUED_STRING_TYPE,
            null
    );

    @Override
    public void init(Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return """
           Enforces the presence and specific values of claim in a JWT.
           - The configured claim must be present in the received JWT.
           - If allowed values are empty, only the presence of the claim is enforced.
           - If allowed values are set, the claim's value must match one of them.
           - Wildcards '*' are supported for flexible value matching (e.g., 'admin*' matches 'admin123').
           - Only claims of type string or number are allowed; multi-valued, arrays, maps, or other JSON objects are not supported.
           """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(CLAIM_NAME_PROPERTY, ALLOWED_VALUES_PROPERTY);
    }
}
