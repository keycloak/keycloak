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
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Collections;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Factory for instances creating {@link AttributeStoreProvider} instances.
 */
public interface AttributeStoreProviderFactory<T extends AttributeStoreProvider> extends ComponentFactory<T, AttributeStoreProvider> {
    String DISPLAY_NAME_METADATA_ATTRIBUTE = "displayName";

    String CONFIG_DISPLAY_NAME = "displayName";

    T create(KeycloakSession session, ComponentModel model);

    @Override
    default void init(Config.Scope config) {}

    @Override
    default void postInit(KeycloakSessionFactory factory) {}

    @Override
    default void close() {}

    @Override
    default String getHelpText() {
        return "";
    }

    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
