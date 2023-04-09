/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ReferenceTypeTokenExecutorFactory extends AbstractReferenceTypeTokenExecutorFactory {

    public static final String PROVIDER_ID = "reference-type-token";

    public static final String SELFCONTAINED_TYPE_TOKEN_BIND_ENDPOINT = "selfcontained-type-token-bind-endpoint";
    public static final String SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT = "selfcontained-token-get-endpoint";

    public static final String SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT_QUERY_PARAM = "reference_type_token";

    private static final ProviderConfigProperty SELFCONTAINED_TYPE_TOKEN_BIND_ENDPOINT_PROPERTY = new ProviderConfigProperty(
            SELFCONTAINED_TYPE_TOKEN_BIND_ENDPOINT, "Self-contained type Token Bind Endpoint", "An endpoint of an external token store for binding a reference type token with an self-contained type token.",
            ProviderConfigProperty.STRING_TYPE, "https://localhost/bind-selfcontained-type-token");

    private static final ProviderConfigProperty SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT_PROPERTY = new ProviderConfigProperty(
            SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT, "Self-contained type Token Get Endpoint", "An endpoint of an external token store for getting an self-contained type token bound with a reference type token.",
            ProviderConfigProperty.STRING_TYPE, "https://localhost/get-selfcontained-type-token");

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new ReferenceTypeTokenExecutor(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Convert self-contained type access and refresh tokens to reference type access and refresh tokens by using an external token store via its endpoints. Converted reference type access and refresh token are the jti claim of corresponding self-contained type access and refresh tokens.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(SELFCONTAINED_TYPE_TOKEN_BIND_ENDPOINT_PROPERTY, SELFCONTAINED_TYPE_TOKEN_GET_ENDPOINT_PROPERTY));
    }

}
