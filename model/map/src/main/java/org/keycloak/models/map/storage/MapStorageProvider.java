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
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

/**
 *
 * @author hmlnarik
 */
public interface MapStorageProvider extends Provider, ProviderFactory<MapStorageProvider> {
    
    public enum Flag {
        INITIALIZE_EMPTY,
        LOCAL
    }

    /**
     * Returns a key-value storage
     * @param <K> type of the primary key
     * @param <V> type of the value
     * @param name Name of the storage
     * @param flags
     * @return
     */
    <K, V extends AbstractEntity<K>> MapStorage<K, V> getStorage(String name, Class<K> keyType, Class<V> valueType, Flag... flags);
}
