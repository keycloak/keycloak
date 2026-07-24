/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.social.microsoft;

import java.util.List;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class MicrosoftIdentityProviderFactory extends AbstractIdentityProviderFactory<MicrosoftIdentityProvider> implements SocialIdentityProviderFactory<MicrosoftIdentityProvider> {

    public static final String PROVIDER_ID = "microsoft";

    @Override
    public String getName() {
        return "Microsoft";
    }

    @Override
    public MicrosoftIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new MicrosoftIdentityProvider(session, new MicrosoftIdentityProviderConfig(model));
    }

    @Override
    public MicrosoftIdentityProviderConfig createConfig() {
        return new MicrosoftIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // The supported authentication URI parameters can be found in the Microsoft identity documentation
        // https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols-oidc#send-the-sign-in-request
        return ProviderConfigurationBuilder.create()
                .property().name("prompt")
                .label("Prompt")
                .helpText("Indicates the type of user interaction that is required. The only valid values at this time are login, none, consent, and select_account.")
                .type(ProviderConfigProperty.STRING_TYPE).add()
                .property().name("tenantId")
                .label("Tenant ID")
                .helpText("Uses single-tenant auth endpoints when specified, uses 'common' multi-tenant endpoints otherwise.")
                .type(ProviderConfigProperty.STRING_TYPE).add()
                .build();
    }
}
