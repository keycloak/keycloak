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

package org.keycloak.models.map.storage.hotRod;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory;

public class HotRodMapStorageProvider implements MapStorageProvider {

    private final HotRodMapStorageProviderFactory factory;
    private final HotRodConnectionProvider connectionProvider;
    private final DeepCloner cloner;

    public HotRodMapStorageProvider(HotRodMapStorageProviderFactory factory, HotRodConnectionProvider connectionProvider, DeepCloner cloner) {
        this.factory = factory;
        this.connectionProvider = connectionProvider;
        this.cloner = cloner;
    }

    @Override
    public <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodMapStorage storage = getHotRodStorage(modelType, flags);
        return storage;
    }

    @SuppressWarnings("unchecked")
    public <E extends AbstractHotRodEntity, V extends HotRodEntityDelegate<E>, M> HotRodMapStorage<String, E, V, M> getHotRodStorage(Class<M> modelType, MapStorageProviderFactory.Flag... flags) {
        HotRodEntityDescriptor<E, V> entityDescriptor = (HotRodEntityDescriptor<E, V>) factory.getEntityDescriptor(modelType);
        return new HotRodMapStorage<>(connectionProvider.getRemoteCache(entityDescriptor.getCacheName()), StringKeyConvertor.StringKey.INSTANCE, entityDescriptor, cloner);
    }

    @Override
    public void close() {

    }
}
