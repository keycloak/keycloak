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

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorageProvider;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorageProvider implements MapStorageProvider {

    private final ConcurrentHashMapStorageProviderFactory factory;

    public ConcurrentHashMapStorageProvider(ConcurrentHashMapStorageProviderFactory factory) {
        this.factory = factory;
    }

    @Override
    public void close() {
    }

    @Override
    public <K, V extends AbstractEntity<K>, M> ConcurrentHashMapStorage<K, V, M> getStorage(
      Class<V> valueType, Class<M> modelType, Flag... flags) {
        return factory.getStorage(valueType, modelType, flags);
    }
}
