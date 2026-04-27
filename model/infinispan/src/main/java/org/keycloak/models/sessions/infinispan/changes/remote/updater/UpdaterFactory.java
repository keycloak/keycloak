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
package org.keycloak.models.sessions.infinispan.changes.remote.updater;

import java.util.Objects;

import org.infinispan.client.hotrod.MetadataValue;

/**
 * A factory interface that creates, wraps or deletes entities.
 *
 * @param <K> The Infinispan key type.
 * @param <V> The Infinispan value type.
 * @param <T> The {@link Updater} concrete type.
 */
public interface UpdaterFactory<K, V, T extends Updater<K, V>> {

    /**
     * Creates an {@link Updater} for an entity created by the current Keycloak transaction.
     *
     * @param key    The Infinispan key.
     * @param entity The Infinispan value.
     * @return The {@link Updater} to be used when updating the entity state.
     */
    T create(K key, V entity);

    /**
     * Wraps an entity read from the Infinispan cache.
     *
     * @param key    The Infinispan key.
     * @param entity The Infinispan value.
     * @return The {@link Updater} to be used when updating the entity state.
     */
    default T wrapFromCache(K key, MetadataValue<V> entity) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(entity);
        return wrapFromCache(key, entity.getValue(), entity.getVersion());
    }

    /**
     * Wraps an entity read from the Infinispan cache.
     *
     * @param key     The Infinispan key.
     * @param value   The Infinispan value.
     * @param version The entry version.
     * @return The {@link Updater} to be used when updating the entity state.
     */
    T wrapFromCache(K key, V value, long version);

    /**
     * Deletes a entity that was not previous read by the Keycloak transaction.
     *
     * @param key The Infinispan key.
     * @return The {@link Updater} for a deleted entity.
     */
    T deleted(K key);
}
