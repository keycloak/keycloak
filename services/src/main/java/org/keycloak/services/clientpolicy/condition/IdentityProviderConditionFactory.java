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

package org.keycloak.services.clientpolicy.condition;

import java.util.List;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * <p>Condition that defines a list of Identity Provider aliases and checks if the
 * alias in the client policy context is (or is not) part of that list.</p>
 *
 * @author rmartinc
 */
public class IdentityProviderConditionFactory extends AbstractClientPolicyConditionProviderFactory {

    public static final String PROVIDER_ID = "identity-provider-alias";
    public static final String IDENTITY_PROVIDERS_ALIASES = "identity_provider_aliases";

    @Override
    public ClientPolicyConditionProvider create(KeycloakSession session) {
        return new IdentityProviderCondition(session);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return """
               Condition that checks the Identity Provider that is involved in the client request.
               Only applies to operations in which an IdP is involved (for example JWT Authorization grant).
               """;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = ProviderConfigurationBuilder.create()
                .property()
                .name(IDENTITY_PROVIDERS_ALIASES)
                .type(ProviderConfigProperty.IDENTITY_PROVIDER_MULTI_LIST_TYPE)
                .label("Identity provider aliases")
                .helpText("List of Identity Provider aliases to take into consideration for the condition.")
                .required(Boolean.TRUE)
                .add()
                .build();
        addCommonConfigProperties(properties);
        return properties;
    }

}
