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

package org.keycloak.models.sessions.infinispan.changes.remote.updater.helper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * An {@link Map} implementation that keeps track of any modification performed in the {@link Map}.
 * <p>
 * The modifications can be replayed in another {@link Map} instance.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class MapUpdater<K, V> extends AbstractMap<K, V> {

    private final Map<K, V> map;
    private final List<Consumer<Map<K, V>>> changes;

    public MapUpdater(Map<K, V> map) {
        this.map = map == null ? new HashMap<>() : map;
        changes = new ArrayList<>(4);
    }

    @Override
    public void clear() {
        changes.clear();
        addChange(Map::clear);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        addChange(kvMap -> kvMap.put(key, value));
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        addChange(kvMap -> kvMap.remove(key));
        return map.remove(key);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    private void addChange(Consumer<Map<K, V>> change) {
        changes.add(change);
    }

    /**
     * Apply the changes tracked into the {@code other} map.
     *
     * @param other The {@link Map} to modify.
     */
    public void applyChanges(Map<K, V> other) {
        changes.forEach(consumer -> consumer.accept(other));
    }

    /**
     * @return {@code true} if this {@link Map} was not modified.
     */
    public boolean isUnchanged() {
        return changes.isEmpty();
    }
}
