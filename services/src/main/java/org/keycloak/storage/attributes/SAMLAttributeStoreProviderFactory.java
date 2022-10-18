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
package org.keycloak.storage.attributes;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.attributeQuery.client.SAMLAttributeQueryClientConfig;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Ben Cresitello-Dittmar
 *
 * This factory creates instances of {@link SAMLAttributeStoreProvider}
 */
public class SAMLAttributeStoreProviderFactory implements AttributeStoreProviderFactory<SAMLAttributeStoreProvider>, EnvironmentDependentProviderFactory {
    private static final String DISPLAY_NAME = "SAML Attribute Store";

    @Override
    public SAMLAttributeStoreProvider create(KeycloakSession session, ComponentModel model) {
        return new SAMLAttributeStoreProvider(this, session, model);
    }

    @Override
    public String getId(){
        return "saml-attribute-store";
    };

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return SAMLAttributeQueryClientConfig.buildProviderConfig();
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
        List<String> errors = SAMLAttributeQueryClientConfig.load(config).verify(session);
        if (!errors.isEmpty()){
            throw new ComponentValidationException(String.format("invalid configuration: %s", String.join("\n", errors)));
        }
    }

    @Override
    public Map<String, Object> getTypeMetadata() {
        // display name metadata used by the UI to display instances
        return Collections.singletonMap(DISPLAY_NAME_METADATA_ATTRIBUTE, DISPLAY_NAME);
    }
    
    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ATTRIBUTE_STORE);
    }
}
