/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.ldap;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.SessionAttributesUtils;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

public class LdapMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final LdapMapStorageProviderFactory factory;
    private final int factoryId;

    public LdapMapStorageProvider(KeycloakSession session, LdapMapStorageProviderFactory factory, int factoryId) {
        this.session = session;
        this.factory = factory;
        this.factoryId = factoryId;
    }

    @Override
    public void close() {
    }

    @Override
    public <V extends AbstractEntity, M> MapStorage<V, M> getMapStorage(Class<M> modelType, Flag... flags) {
        return SessionAttributesUtils.createMapStorageIfAbsent(session, getClass(), modelType, factoryId, () -> {
            LdapMapStorage store = (LdapMapStorage) factory.createMapStorage(session, modelType);
            session.getTransactionManager().enlist(store);
            return store;
        });
    }

}
