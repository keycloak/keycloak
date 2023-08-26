/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.SessionAttributesUtils;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.CrudOperations;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

import static org.keycloak.models.map.storage.chm.ConcurrentHashMapStorageProviderFactory.CLONER;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProvider implements MapStorageProvider {

    private final KeycloakSession session;
    private final ConcurrentHashMapStorageProviderFactory factory;
    private final int factoryId;

    public ConcurrentHashMapStorageProvider(KeycloakSession session, ConcurrentHashMapStorageProviderFactory factory, int factoryId) {
        this.session = session;
        this.factory = factory;
        this.factoryId = factoryId;
    }

    @Override
    public void close() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends AbstractEntity, M> MapStorage<V, M> getMapStorage(Class<M> modelType, Flag... flags) {
        return SessionAttributesUtils.createMapStorageIfAbsent(session, getClass(), modelType, factoryId, () -> {
            ConcurrentHashMapStorage store = getMapStorage(modelType, factory.getStorage(modelType, flags));
            session.getTransactionManager().enlist(store);
            return store;
        });
    }

    private <K, V extends AbstractEntity & UpdatableEntity, M> ConcurrentHashMapStorage getMapStorage(Class<?> modelType, ConcurrentHashMapCrudOperations<K, V, M> crud) {
        if (modelType == SingleUseObjectValueModel.class) {
            return new SingleUseObjectMapStorage(crud, factory.getKeyConverter(modelType), CLONER, MapFieldPredicates.getPredicates(modelType));
        }
        return new ConcurrentHashMapStorage(crud, factory.getKeyConverter(modelType), CLONER, MapFieldPredicates.getPredicates(modelType));
    }
}
