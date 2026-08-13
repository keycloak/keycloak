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
package org.keycloak.testsuite.federation;

import java.util.List;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.UserStorageProviderFactory;

/**
 * Factory for FailingUserStorageProvider - used in tests to simulate 
 * user storage provider failures for graceful degradation testing.
 */
public class FailingUserStorageProviderFactory implements UserStorageProviderFactory<FailingUserStorageProvider> {
    
    public static final String PROVIDER_ID = "failing-user-storage";
    
    @Override
    public FailingUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        return new FailingUserStorageProvider(session, model);
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getHelpText() {
        return "Test user storage provider that can be configured to fail for testing graceful degradation";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
            .property()
                .name(FailingUserStorageProvider.FAIL_ON_SEARCH)
                .label("Fail on Search")
                .helpText("If enabled, this provider will throw exceptions during user search operations")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
            .property()
                .name(FailingUserStorageProvider.FAIL_ON_COUNT)
                .label("Fail on Count")
                .helpText("If enabled, this provider will throw exceptions during user count operations")
                .type(ProviderConfigProperty.BOOLEAN_TYPE)
                .defaultValue(false)
                .add()
            .build();
    }
    
    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        // No validation needed for test provider
    }
}