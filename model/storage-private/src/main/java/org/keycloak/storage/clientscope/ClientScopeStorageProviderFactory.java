/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.clientscope;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.keycloak.Config;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;

public interface ClientScopeStorageProviderFactory<T extends ClientScopeStorageProvider> extends ComponentFactory<T, ClientScopeStorageProvider> {


    /**
     * called per Keycloak transaction.
     *
     * @param session
     * @param model
     * @return
     */
    @Override
    T create(KeycloakSession session, ComponentModel model);

    /**
     * This is the name of the provider.
     *
     * @return
     */
    @Override
    String getId();

    @Override
    default void init(Config.Scope config) {
    }

    @Override
    default void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    default void close() {
    }

    @Override
    default String getHelpText() {
        return "";
    }

    @Override
    default List<ProviderConfigProperty> getConfigProperties() {
        return Collections.EMPTY_LIST;
    }

    @Override
    default void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config) throws ComponentValidationException {
    }

    /**
     * Called when ClientScopeStorageProviderFactory is created.  This allows you to do initialization of any additional configuration
     * you need to add.
     *
     * @param session
     * @param realm
     * @param model
     */
    @Override
    default void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
    }

    /**
     * configuration properties that are common across all ClientScopeStorageProvider implementations
     *
     * @return
     */
    @Override
    default List<ProviderConfigProperty> getCommonProviderConfigProperties() {
        return ClientScopeStorageProviderSpi.commonConfig();
    }

    @Override
    default
    Map<String, Object> getTypeMetadata() {
        return new HashMap<>();
    }
}
