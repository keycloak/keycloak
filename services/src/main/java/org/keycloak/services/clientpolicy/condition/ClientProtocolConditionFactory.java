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

package org.keycloak.services.clientpolicy.condition;

import java.util.LinkedList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 *
 * @author rmartinc
 */
public class ClientProtocolConditionFactory implements ClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "client-type";

    private List<String> loginProtocols;

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new ClientProtocolCondition(session);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        try (KeycloakSession session = factory.create()) {
            loginProtocols = new LinkedList<>(session.listProviderIds(LoginProtocol.class));
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "Condition that uses the client's protocol (OpenID Connect, SAML) to determine whether the policy is applied.";
    }

    @Override
    public void init(Config.Scope config) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name("protocol")
                .type(ProviderConfigProperty.LIST_TYPE)
                .options(loginProtocols)
                .defaultValue(loginProtocols.iterator().next())
                .label("Client protocol")
                .helpText("What client login protocol the condition will apply on.")
                .add()
                .build();
    }
}
