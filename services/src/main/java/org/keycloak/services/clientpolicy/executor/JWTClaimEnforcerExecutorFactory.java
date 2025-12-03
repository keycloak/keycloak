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

    public static final String ALLOWED_VALUE = "allowed-value";

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

    private static final ProviderConfigProperty ALLOWED_VALUE_PROPERTY = new ProviderConfigProperty(
            ALLOWED_VALUE,
            "Allowed Value",
            "Value that the JWT claim must match. Regular expressions are supported. If left empty, only the presence of the claim is enforced.",
            ProviderConfigProperty.STRING_TYPE,
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
        The executor enforces the presence and specific values of a claim in a JWT.
        It is applied in client requests where an existing JWT token is passed, such as a JWT Authorization Grant request (RFC 7523) or Standard Token Exchange request.
        The configured claim must be present in the received JWT.
        If allowed value is empty, only the presence of the claim is enforced.
        If allowed value is set, the claim's value must match the configured regular expression.
        Only claims of type string or number are allowed; multi-valued, arrays, maps, or other JSON objects are not supported.
        """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(CLAIM_NAME_PROPERTY, ALLOWED_VALUE_PROPERTY);
    }
}
