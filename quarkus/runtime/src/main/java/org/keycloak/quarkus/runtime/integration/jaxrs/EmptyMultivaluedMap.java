/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.integration.jaxrs;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.core.MultivaluedMap;

public final class EmptyMultivaluedMap<K, V> implements MultivaluedMap<K, V> {

    @Override
    public void putSingle(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V getFirst(K key) {
        return null;
    }

    @Override
    public void addAll(K key, V... newValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(K key, List<V> valueList) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFirst(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equalsIgnoreValueOrder(MultivaluedMap<K, V> otherMap) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public List<V> get(Object key) {
        return null;
    }

    @Override
    public List<V> put(K key, List<V> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<V> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends List<V>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        return emptySet();
    }

    @Override
    public Collection<List<V>> values() {
        return emptySet();
    }

    @Override
    public Set<Entry<K, List<V>>> entrySet() {
        return emptySet();
    }
}
