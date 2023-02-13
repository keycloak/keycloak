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

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecureSigningAlgorithmForSignedJwtExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "secure-signature-algorithm-signed-jwt";

    public static final String REQUIRE_CLIENT_ASSERTION = "require-client-assertion";

    private static final ProviderConfigProperty REQUIRE_CLIENT_ASSERTION_PROPERTY = new ProviderConfigProperty(
            REQUIRE_CLIENT_ASSERTION, "Require Client Assertion", "If this is ON, then parameter 'client_assertion' will be required in the requests and request will fail if it is not present. " +
            "If false, then parameter 'client_assertion' is not required in the requests, which is convenient for example for clients authenticating with MTLS. When 'client_assertion' parameter is present in the request, " +
            "then the algorithm on the JWT from specified client assertion is always checked regardless of the value of this switch", ProviderConfigProperty.BOOLEAN_TYPE, false);

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new SecureSigningAlgorithmForSignedJwtExecutor(session);
    }

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
        return "It refuses the client whose JWT token signature algorithms are considered not to be secure. It accepts ES256, ES384, ES512, PS256, PS384 and PS512.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(REQUIRE_CLIENT_ASSERTION_PROPERTY));
    }

}
