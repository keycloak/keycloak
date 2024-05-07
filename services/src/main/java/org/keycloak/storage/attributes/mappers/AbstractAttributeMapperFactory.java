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
package org.keycloak.storage.attributes.mappers;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentModelScope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.storage.attributes.AttributeMapper;
import org.keycloak.storage.attributes.AttributeMapperFactory;

import java.util.Objects;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for creating instances of the {@link AbstractAttributeMapper} provider
 *
 * @param <T> The type of attribute mapper this factory creates
 */
public abstract class AbstractAttributeMapperFactory<T extends AttributeMapper> implements AttributeMapperFactory<T>, EnvironmentDependentProviderFactory {
    private ComponentModelScope  componentConfig;
    private Config.Scope config;

    @Override
    public T create(KeycloakSession session, ComponentModel model) {
        // instantiate mapper
        Config.Scope scope = Config.scope(model.getProviderId());
        ComponentModelScope configScope = new ComponentModelScope(scope, model);
        return createMapper(configScope);
    }

    @Override
    public T create(KeycloakSession session) {
        return createMapper(componentConfig);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
        if (config instanceof ComponentModelScope){
            this.componentConfig = (ComponentModelScope) config;
        } else {
            this.componentConfig = new ComponentModelScope(config, new ComponentModel());
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ATTRIBUTE_STORE);
    }



    @Override
    public String getHelpText() {
        return null;
    }

    /**
     * A helper function to determine if the configuration key is set on the provided component model.
     * @param configKey The key in the model to get
     * @param model The model to fetch the configKey from
     * @return True if the model contains a value for the specified configKey
     */
    protected boolean isSet(String configKey, ComponentModel model){
        return model.get(configKey) != null && !Objects.equals(model.get(configKey), "");
    }

    /**
     * Helper interface to create an instance of the given mapper. This must be implemented by the extending class.
     * @param config the mapper component configuration
     * @return the initialized mapper instance
     */
    protected abstract T createMapper(ComponentModelScope config);

}