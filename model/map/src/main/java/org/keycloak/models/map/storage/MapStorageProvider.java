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
package org.keycloak.models.map.storage;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorageProviderFactory.Flag;
import org.keycloak.provider.Provider;

/**
 *
 * @author hmlnarik
 */
public interface MapStorageProvider extends Provider {
    
    /**
     * Returns a key-value storage implementation for the particular types.
     * @param <K> type of the primary key
     * @param <V> type of the value
     * @param name Name of the storage
     * @param flags
     * @return
     * @throws IllegalArgumentException If some of the types is not supported by the underlying implementation.
     */
    <K, V extends AbstractEntity<K>, M> MapStorage<K, V, M> getStorage(Class<V> valueType, Class<M> modelType, Flag... flags);
}
