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
     * Returns a key-value storage implementation for the given types.
     * @param <V> type of the value
     * @param <M> type of the corresponding model (e.g. {@code UserModel})
     * @param modelType Model type
     * @param flags Flags of the returned storage. Best effort, flags may be not honored by underlying implementation
     * @return
     * @throws IllegalArgumentException If some of the types is not supported by the underlying implementation.
     */
    <V extends AbstractEntity, M> MapStorage<V, M> getStorage(Class<M> modelType, Flag... flags);
}
