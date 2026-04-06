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
package org.keycloak.federation.scim;

import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.List;

/**
 * Factory for SCIM User Storage Provider.
 * Enabled behind experimental feature profile.
 */
public class ScimUserStorageProviderFactory implements UserStorageProviderFactory<ScimUserStorageProvider> {

    public static final String PROVIDER_ID = "scim";
    public static final String SCIM_FEATURE = "scim";

    @Override
    public ScimUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new ScimUserStorageProvider(session, model, session.getContext().getRealm());
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "SCIM 2.0 User Storage Provider for connecting to SCIM Service Providers";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
            new ProviderConfigProperty("scimBaseUrl", "SCIM Base URL", 
                "Base URL of the SCIM service provider", ProviderConfigProperty.STRING_TYPE, null),
            new ProviderConfigProperty("scimAuthType", "Authentication Type",
                "Authentication type (oauth, basic, bearer)", ProviderConfigProperty.STRING_TYPE, "bearer"),
            new ProviderConfigProperty("scimAuthToken", "Authentication Token",
                "Token for authentication (if using bearer or oauth)", ProviderConfigProperty.PASSWORD, null),
            new ProviderConfigProperty("scimUsername", "Username",
                "Username for basic authentication", ProviderConfigProperty.STRING_TYPE, null),
            new ProviderConfigProperty("scimPassword", "Password",
                "Password for basic authentication", ProviderConfigProperty.PASSWORD, null)
        );
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.EXPERIMENTAL) || 
               Profile.isFeatureEnabled(Profile.Feature.SCIM);
    }
}
