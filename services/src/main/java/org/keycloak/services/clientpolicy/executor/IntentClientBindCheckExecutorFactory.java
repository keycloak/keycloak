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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class IntentClientBindCheckExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "intent-client-bind-checker";

    public static final String INTENT_CLIENT_BIND_CHECK_ENDPOINT = "intent-client-bind-check-endpoint";

    private static final ProviderConfigProperty INTENT_CLIENT_BIND_CHECK_ENDPOINT_PROPERTY = new ProviderConfigProperty(
            INTENT_CLIENT_BIND_CHECK_ENDPOINT, "Intent Client Bind Check Endpoint", "Endpoint for checking if openbanking_intent_id is bound with a client.",
            ProviderConfigProperty.STRING_TYPE, "https://rs.keycloak-fapi.org/check-intent-client-bound");

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new IntentClientBindCheckExecutor(session);
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
        return "The executor checks if openbanking_intent_id is bound with a client.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new ArrayList<>(Arrays.asList(INTENT_CLIENT_BIND_CHECK_ENDPOINT_PROPERTY));
    }

}
