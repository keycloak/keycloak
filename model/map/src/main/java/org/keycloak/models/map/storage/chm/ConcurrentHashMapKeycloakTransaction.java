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
package org.keycloak.models.map.storage.chm;

import org.keycloak.models.map.common.AbstractEntity;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;

public class ConcurrentHashMapKeycloakTransaction<K, V extends AbstractEntity<K>, M> implements MapKeycloakTransaction<K, V, M> {

    private final static Logger log = Logger.getLogger(ConcurrentHashMapKeycloakTransaction.class);

    private boolean active;
    private boolean rollback;
    private final Map<K, MapTaskWithValue> tasks = new LinkedHashMap<>();
    private final MapStorage<K, V, M> map;

    enum MapOperation {
        CREATE, UPDATE, DELETE,
    }

    public ConcurrentHashMapKeycloakTransaction(MapStorage<K, V, M> map) {
        this.map = map;
    }

    @Override
    public void begin() {
        active = true;
    }

    @Override
    public void commit() {
        log.tracef("Commit - %s", map);

        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }

        for (MapTaskWithValue value : tasks.values()) {
            value.execute();
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
    protected void addTask(K key, MapTaskWithValue task) {
        log.tracef("Adding operation %s for %s @ %08x", task.getOperation(), key, System.identityHashCode(task.getValue()));

        K taskKey = key;
        tasks.merge(taskKey, task, MapTaskCompose::new);
    }

    @Override
    public V read(K key) {
        try {   // TODO: Consider using Optional rather than handling NPE
            return read(key, map::read);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    @Override
    public V read(K key, Function<K, V> defaultValueFunc) {
        MapTaskWithValue current = tasks.get(key);
        // If the key exists, then it has entered the "tasks" after bulk delete that could have 
        // removed it, so looking through bulk deletes is irrelevant
        if (tasks.containsKey(key)) {
            return current.getValue();
        }

        // If the key does not exist, then it would be read fresh from the storage, but then it
        // could have been removed by some bulk delete in the existing tasks. Check it.
        final V value = defaultValueFunc.apply(key);
        for (MapTaskWithValue val : tasks.values()) {
            if (val instanceof ConcurrentHashMapKeycloakTransaction.BulkDeleteOperation) {
                final BulkDeleteOperation delOp = (BulkDeleteOperation) val;
                if (! delOp.getFilterForNonDeletedObjects().test(value)) {
                    return null;
                }
            }
        }

        return value;
    }

    /**
     * Returns the stream of records that match given criteria and includes changes made in this transaction, i.e.
     * the result contains updates and excludes records that have been deleted in this transaction.
     *
     * @param mcb
     * @return
     */
    @Override
    public Stream<V> read(ModelCriteriaBuilder<M> mcb) {
        Predicate<? super V> filterOutAllBulkDeletedObjects = tasks.values().stream()
          .filter(BulkDeleteOperation.class::isInstance)
          .map(BulkDeleteOperation.class::cast)
          .map(BulkDeleteOperation::getFilterForNonDeletedObjects)
          .reduce(Predicate::and)
          .orElse(v -> true);

        Stream<V> updatedAndNotRemovedObjectsStream = this.map.read(mcb)
          .filter(filterOutAllBulkDeletedObjects)
          .map(this::getUpdated)      // If the object has been removed, tx.get will return null, otherwise it will return me.getValue()
          .filter(Objects::nonNull);

        // In case of created values stored in MapKeycloakTransaction, we need filter those according to the filter
        MapModelCriteriaBuilder<K, V, M> mapMcb = mcb.unwrap(MapModelCriteriaBuilder.class);
        Stream<V> res = mapMcb == null
          ? updatedAndNotRemovedObjectsStream
          : Stream.concat(
              createdValuesStream(mapMcb.getKeyFilter(), mapMcb.getEntityFilter()),
              updatedAndNotRemovedObjectsStream
            );

        return res;
    }

    @Override
    public long getCount(ModelCriteriaBuilder<M> mcb) {
        return read(mcb).count();
    }

    @Override
    public V getUpdated(V orig) {
        MapTaskWithValue current = orig == null ? null : tasks.get(orig.getId());
        return current == null ? orig : current.getValue();
    }

    @Override
    public void update(K key, V value) {
        addTask(key, new UpdateOperation(key, value));
    }

    @Override
    public void create(K key, V value) {
        addTask(key, new CreateOperation(key, value));
    }

    @Override
    public void updateIfChanged(K key, V value, Predicate<V> shouldPut) {
        log.tracef("Adding operation UPDATE_IF_CHANGED for %s @ %08x", key, System.identityHashCode(value));

        K taskKey = key;
        MapTaskWithValue op = new MapTaskWithValue(value) {
            @Override
            public void execute() {
                if (shouldPut.test(getValue())) {
                    map.update(key, getValue());
                }
            }
            @Override public MapOperation getOperation() { return MapOperation.UPDATE; }
        };
        tasks.merge(taskKey, op, this::merge);
    }

    @Override
    public void delete(K key) {
        addTask(key, new DeleteOperation(key));
    }


    @Override
    public long delete(K artificialKey, ModelCriteriaBuilder<M> mcb) {
        log.tracef("Adding operation DELETE_BULK");

        // Remove all tasks that create / update / delete objects deleted by the bulk removal.
        final BulkDeleteOperation bdo = new BulkDeleteOperation(mcb);
        Predicate<V> filterForNonDeletedObjects = bdo.getFilterForNonDeletedObjects();
        long res = 0;
        for (Iterator<Entry<K, MapTaskWithValue>> it = tasks.entrySet().iterator(); it.hasNext();) {
            Entry<K, MapTaskWithValue> me = it.next();
            if (! filterForNonDeletedObjects.test(me.getValue().getValue())) {
                log.tracef(" [DELETE_BULK] removing %s", me.getKey());
                it.remove();
                res++;
            }
        }

        tasks.put(artificialKey, bdo);

        return res + bdo.getCount();
    }

    private Stream<V> createdValuesStream(Predicate<? super K> keyFilter, Predicate<? super V> entityFilter) {
        return this.tasks.entrySet().stream()
          .filter(me -> keyFilter.test(me.getKey()))
          .map(Map.Entry::getValue)
          .filter(v -> v.containsCreate() && ! v.isReplace())
          .map(MapTaskWithValue::getValue)
          .filter(Objects::nonNull)
          .filter(entityFilter)
          // make a snapshot
          .collect(Collectors.toList()).stream();
    }

    private MapTaskWithValue merge(MapTaskWithValue oldValue, MapTaskWithValue newValue) {
        switch (newValue.getOperation()) {
            case DELETE:
                return oldValue.containsCreate() ? null : newValue;
            default:
                return new MapTaskCompose(oldValue, newValue);
        }
    }

    protected abstract class MapTaskWithValue {
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
        public abstract void execute();
   }

    private class MapTaskCompose extends MapTaskWithValue {

        private final MapTaskWithValue oldValue;
        private final MapTaskWithValue newValue;

        public MapTaskCompose(MapTaskWithValue oldValue, MapTaskWithValue newValue) {
            super(null);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public void execute() {
            oldValue.execute();
            newValue.execute();
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
              (oldValue instanceof ConcurrentHashMapKeycloakTransaction.MapTaskCompose && ((MapTaskCompose) oldValue).isReplace());
        }
    }

    private class CreateOperation extends MapTaskWithValue {
        private final K key;

        public CreateOperation(K key, V value) {
            super(value);
            this.key = key;
        }

        @Override public void execute() { map.create(key, getValue()); }
        @Override public MapOperation getOperation() { return MapOperation.CREATE; }
    }

    private class UpdateOperation extends MapTaskWithValue {
        private final K key;

        public UpdateOperation(K key, V value) {
            super(value);
            this.key = key;
        }

        @Override public void execute() { map.update(key, getValue()); }
        @Override public MapOperation getOperation() { return MapOperation.UPDATE; }
    }

    private class DeleteOperation extends MapTaskWithValue {
        private final K key;

        public DeleteOperation(K key) {
            super(null);
            this.key = key;
        }

        @Override public void execute() { map.delete(key); }
        @Override public MapOperation getOperation() { return MapOperation.DELETE; }
    }

    private class BulkDeleteOperation extends MapTaskWithValue {

        private final ModelCriteriaBuilder<M> mcb;

        public BulkDeleteOperation(ModelCriteriaBuilder<M> mcb) {
            super(null);
            this.mcb = mcb;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void execute() {
            map.delete(mcb);
        }

        public Predicate<V> getFilterForNonDeletedObjects() {
            if (! (mcb instanceof MapModelCriteriaBuilder)) {
                return t -> true;
            }

            @SuppressWarnings("unchecked")
            final MapModelCriteriaBuilder<K, V, M> mmcb = (MapModelCriteriaBuilder<K, V, M>) mcb;
            
            Predicate<? super V> entityFilter = mmcb.getEntityFilter();
            Predicate<? super K> keyFilter = ((MapModelCriteriaBuilder) mcb).getKeyFilter();
            return v -> v == null || ! (keyFilter.test(v.getId()) && entityFilter.test(v));
        }

        @Override
        public MapOperation getOperation() {
            return MapOperation.DELETE;
        }

        private long getCount() {
            return map.getCount(mcb);
        }
    }
}