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

package org.keycloak.models.sessions.infinispan.changes.remote;

import java.util.Map;
import java.util.Objects;

import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.Updater;

/**
 * Represents a predicate to mass remove entries from a {@link RemoteCache}.
 *
 * @param <K> The key's type stored in the {@link RemoteCache}.
 * @param <V> The value's type stored in the {@link RemoteCache}.
 */
public interface RemoveEntryPredicate<K, V> {

    /**
     * @param key   The entry's key to test.
     * @param value The entry's value to test.
     * @return {@code true} if the entry should be removed from the {@link RemoteCache}.
     */
    boolean shouldRemove(K key, V value);

    /**
     * @param entry The {@link RemoteCache} entry to test.
     * @return {@code true} if the entry should be removed from the {@link RemoteCache}.
     * @see #shouldRemove(Object, Object)
     */
    default boolean shouldRemove(Map.Entry<K, MetadataValue<V>> entry) {
        return shouldRemove(entry.getKey(), entry.getValue().getValue());
    }

    /**
     * @param updater The {@link Updater} to test.
     * @return {@code true} if the entry tracked by the {@link Updater} should be removed from the {@link RemoteCache}.
     */
    default boolean shouldRemove(Updater<K, V> updater) {
        return shouldRemove(updater.getKey(), updater.getValue());
    }

    /**
     * Logical 'OR' between this {@link RemoveEntryPredicate} and {@code other}.
     *
     * @param other The other {@link RemoveEntryPredicate} to be logical 'OR' with.
     * @return A {@link RemoveEntryPredicate} with a logical 'OR' between {@code this} and {@code other}.
     */
    default RemoveEntryPredicate<K, V> or(RemoveEntryPredicate<K, V> other) {
        Objects.requireNonNull(other);
        return (k, v) -> shouldRemove(k, v) || other.shouldRemove(k, v);
    }

}
