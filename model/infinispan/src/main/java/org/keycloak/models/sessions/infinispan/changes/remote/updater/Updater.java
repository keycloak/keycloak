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


import java.util.function.BiFunction;

import org.keycloak.models.sessions.infinispan.remote.transaction.RemoteChangeLogTransaction;

/**
 * An interface used by {@link RemoteChangeLogTransaction}.
 * <p>
 * It keeps track of the changes made in the entity and applies them to the entity stored in Infinispan cache.
 *
 * @param <K> The Infinispan key type.
 * @param <V> The Infinispan value type.
 */
public interface Updater<K, V> extends BiFunction<K, V, V> {

    int NO_VERSION = -1;

    /**
     * @return The Infinispan cache key.
     */
    K getKey();

    /**
     * @return The up-to-date entity used by the transaction.
     */
    V getValue();

    /**
     * @return The entity version when reading for the first time from Infinispan.
     */
    long getVersionRead();

    /**
     * @return {@code true} if the entity was removed during the Keycloak transaction and it should be removed from
     * Infinispan.
     */
    boolean isDeleted();

    /**
     * @return {@code true} if the entity was created during the Keycloak transaction. Allows some optimization like
     * put-if-absent.
     */
    boolean isCreated();

    /**
     * @return {@code true} if the entity was not changed.
     */
    boolean isReadOnly();

    /**
     * @return {@code true} if the entity is expired.
     */
    boolean isExpired();

    /**
     * @return {@code true} if the entity is not valid and cannot be viewed/accessed from the transaction.
     */
    default boolean isInvalid() {
        return isExpired() || isDeleted();
    }

    /**
     * Marks the entity as deleted.
     */
    void markDeleted();

    /**
     * Marks the entity as expired when loading from the Infinispan cache.
     */
    void markExpired();

    /**
     * @return {@code true} if the entity is transient and shouldn't be stored in the Infinispan cache.
     */
    default boolean isTransient() {
        return false;
    }

    /**
     * Computes the expiration data for Infinispan cache.
     *
     * @return The {@link Expiration} data.
     */
    Expiration computeExpiration();

    default boolean hasVersion() {
        return getVersionRead() != NO_VERSION;
    }
}
