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

import org.keycloak.models.KeycloakTransaction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jboss.logging.Logger;

public class MapKeycloakTransaction<K, V> implements KeycloakTransaction {

    private final static Logger log = Logger.getLogger(MapKeycloakTransaction.class);

    private enum MapOperation {
        CREATE {
            @Override
            protected <K, V> MapTaskWithValue<K, V> taskFor(K key, V value) {
                return new MapTaskWithValue<K, V>(value) {
                    @Override public void execute(MapStorage<K, V> map) { map.putIfAbsent(key, getValue()); }
                    @Override public MapOperation getOperation() { return CREATE; }
                };
            }
        },
        UPDATE {
            @Override
            protected <K, V> MapTaskWithValue<K, V> taskFor(K key, V value) {
                return new MapTaskWithValue<K, V>(value) {
                    @Override public void execute(MapStorage<K, V> map) { map.put(key, getValue()); }
                    @Override public MapOperation getOperation() { return UPDATE; }
                };
            }
        },
        DELETE {
            @Override
            protected <K, V> MapTaskWithValue<K, V> taskFor(K key, V value) {
                return new MapTaskWithValue<K, V>(null) {
                    @Override public void execute(MapStorage<K, V> map) { map.remove(key); }
                    @Override public MapOperation getOperation() { return DELETE; }
                };
            }
        },
        ;

        protected abstract <K, V> MapTaskWithValue<K, V> taskFor(K key, V value);

    }

    private boolean active;
    private boolean rollback;
    private final Map<K, MapTaskWithValue<K, V>> tasks = new LinkedHashMap<>();
    private final MapStorage<K, V> map;

    public MapKeycloakTransaction(MapStorage<K, V> map) {
        this.map = map;
    }

    @Override
    public void begin() {
        active = true;
    }

    @Override
    public void commit() {
        log.trace("Commit");

        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }

        for (MapTaskWithValue<K, V> value : tasks.values()) {
            value.execute(map);
        }
    }

    @Override
    public void rollback() {
        tasks.clear();
    }

    @Override
    public void setRollbackOnly() {
        rollback = true;
    }

    @Override
    public boolean getRollbackOnly() {
        return rollback;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    /**
     * Adds a given task if not exists for the given key
     */
    private void addTask(MapOperation op, K key, V value) {
        log.tracef("Adding operation %s for %s  @ %08x", op, key, System.identityHashCode(value));

        K taskKey = key;
        tasks.merge(taskKey, op.taskFor(key, value), MapTaskCompose::new);
    }

    // This is for possibility to lookup for session by id, which was created in this transaction
    public V get(K key, Function<K, V> defaultValueFunc) {
        MapTaskWithValue<K, V> current = tasks.get(key);
        if (current != null) {
            return current.getValue();
        }

        return defaultValueFunc.apply(key);
    }

    public V getUpdated(Map.Entry<K, V> keyDefaultValue) {
        MapTaskWithValue<K, V> current = tasks.get(keyDefaultValue.getKey());
        if (current != null) {
            return current.getValue();
        }

        return keyDefaultValue.getValue();
    }

    public void put(K key, V value) {
        addTask(MapOperation.UPDATE, key, value);
    }

    public void putIfAbsent(K key, V value) {
        addTask(MapOperation.CREATE, key, value);
    }

    public void putIfChanged(K key, V value, Predicate<V> shouldPut) {
        log.tracef("Adding operation UPDATE_IF_CHANGED for %s  @ %08x", key, System.identityHashCode(value));

        K taskKey = key;
        MapTaskWithValue<K, V> op = new MapTaskWithValue<K, V>(value) {
            @Override
            public void execute(MapStorage<K, V> map) {
                if (shouldPut.test(getValue())) {
                    map.put(key, getValue());
                }
            }
            @Override public MapOperation getOperation() { return MapOperation.UPDATE; }
        };
        tasks.merge(taskKey, op, MapKeycloakTransaction::merge);
    }

    public void remove(K key) {
        addTask(MapOperation.DELETE, key, null);
    }

    public Stream<V> valuesStream() {
        return this.tasks.values().stream()
          .map(MapTaskWithValue<K,V>::getValue)
          .filter(Objects::nonNull);
    }

    public Stream<V> createdValuesStream() {
        return this.tasks.values().stream()
          .filter(v -> v.containsCreate() && ! v.isReplace())
          .map(MapTaskWithValue<K,V>::getValue)
          .filter(Objects::nonNull);
    }

    private static <K, V> MapTaskWithValue<K, V> merge(MapTaskWithValue<K, V> oldValue, MapTaskWithValue<K, V> newValue) {
        switch (newValue.getOperation()) {
            case DELETE:
                return oldValue.containsCreate() ? null : newValue;
            default:
                return new MapTaskCompose<>(oldValue, newValue);
        }
    }

    private static abstract class MapTaskWithValue<K, V> {
        protected final V value;

        public MapTaskWithValue(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

        public boolean containsCreate() {
            return MapOperation.CREATE == getOperation();
        }

        public boolean containsRemove() {
            return MapOperation.DELETE == getOperation();
        }

        public boolean isReplace() {
            return false;
        }

        public abstract MapOperation getOperation();
        public abstract void execute(MapStorage<K,V> map);
   }

    private static class MapTaskCompose<K, V> extends MapTaskWithValue<K, V> {

        private final MapTaskWithValue<K, V> oldValue;
        private final MapTaskWithValue<K, V> newValue;

        public MapTaskCompose(MapTaskWithValue<K, V> oldValue, MapTaskWithValue<K, V> newValue) {
            super(null);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void execute(MapStorage<K, V> map) {
            oldValue.execute(map);
            newValue.execute(map);
        }

        @Override
        public V getValue() {
            return newValue.getValue();
        }

        @Override
        public MapOperation getOperation() {
            return null;
        }

        @Override
        public boolean containsCreate() {
            return oldValue.containsCreate() || newValue.containsCreate();
        }

        @Override
        public boolean containsRemove() {
            return oldValue.containsRemove() || newValue.containsRemove();
        }

        @Override
        public boolean isReplace() {
            return (newValue.getOperation() == MapOperation.CREATE && oldValue.containsRemove()) ||
              (oldValue instanceof MapTaskCompose && ((MapTaskCompose) oldValue).isReplace());
        }
    }
}