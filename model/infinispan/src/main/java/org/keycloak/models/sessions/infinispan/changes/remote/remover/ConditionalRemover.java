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

package org.keycloak.models.sessions.infinispan.changes.remote.remover;

import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;
import org.keycloak.models.sessions.infinispan.remote.transaction.RemoteChangeLogTransaction;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.AggregateCompletionStage;

/**
 * It handles conditional remove operations.
 * <p>
 * This class is preferred to remove an unknown amount of entries by its key and/or value state. The implement may use
 * queries (delete statements) or perform a full cache scan to find the entries to remove.
 * <p>
 * The method {@link #willRemove(Updater)} is invoked by {@link RemoteChangeLogTransaction} before perform any change
 * tracked by the {@link Updater}. This is an optimization to prevent sending changes that would be removed by this
 * {@link ConditionalRemover}.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
public interface ConditionalRemover<K, V> {

    /**
     * @param key   The entry's key to test.
     * @param value The entry's value to test.
     * @return {@code true} if the entry will be removed from the {@link RemoteCache}.
     */
    boolean willRemove(K key, V value);

    /**
     * @param updater The {@link Updater} to test.
     * @return {@code true} if the entry tracked by the {@link Updater} will be removed from the {@link RemoteCache}.
     */
    default boolean willRemove(Updater<K, V> updater) {
        // The value can be null if the entry updated is marked as deleted.
        // In that case, we don't have the value to check for the condition and will let the transaction perform the removal.
        return updater.getValue() != null && willRemove(updater.getKey(), updater.getValue());
    }

    /**
     * Executes the conditional removes in the {@link RemoteCache}.
     *
     * @param cache The {@link RemoteCache} to perform the remove operations.
     * @param stage The {@link AggregateCompletionStage} to add any incomplete tasks.
     */
    void executeRemovals(RemoteCache<K, V> cache, AggregateCompletionStage<Void> stage);

}
