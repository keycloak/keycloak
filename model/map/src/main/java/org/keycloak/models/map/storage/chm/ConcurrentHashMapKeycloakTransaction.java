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

import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
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
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.storage.SearchableModelField;

public class ConcurrentHashMapKeycloakTransaction<K, V extends AbstractEntity & UpdatableEntity, M> implements MapKeycloakTransaction<V, M> {

    private final static Logger log = Logger.getLogger(ConcurrentHashMapKeycloakTransaction.class);

    protected boolean active;
    protected boolean rollback;
    protected final Map<String, MapTaskWithValue> tasks = new LinkedHashMap<>();
    protected final ConcurrentHashMapCrudOperations<V, M> map;
    protected final StringKeyConverter<K> keyConverter;
    protected final DeepCloner cloner;
    protected final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;

    enum MapOperation {
        CREATE, UPDATE, DELETE,
    }

    public ConcurrentHashMapKeycloakTransaction(ConcurrentHashMapCrudOperations<V, M> map, StringKeyConverter<K> keyConverter, DeepCloner cloner, Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates) {
        this.map = map;
        this.keyConverter = keyConverter;
        this.cloner = cloner;
        this.fieldPredicates = fieldPredicates;
    }

    @Override
    public void begin() {
        active = true;
    }

    @Override
    public void commit() {
        if (rollback) {
            throw new RuntimeException("Rollback only!");
        }

        if (! tasks.isEmpty()) {
            log.tracef("Commit - %s", map);
            for (MapTaskWithValue value : tasks.values()) {
                value.execute();
            }
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

    private MapModelCriteriaBuilder<K, V, M> createCriteriaBuilder() {
        return new MapModelCriteriaBuilder<K, V, M>(keyConverter, fieldPredicates);
    }

    /**
     * Adds a given task if not exists for the given key
     */
    protected void addTask(String key, MapTaskWithValue task) {
        log.tracef("Adding operation %s for %s @ %08x", task.getOperation(), key, System.identityHashCode(task.getValue()));

        tasks.merge(key, task, MapTaskCompose::new);
    }

    /**
     * Returns a deep clone of an entity. If the clone is already in the transaction, returns this one.
     * <p>
     * Usually used before giving an entity from a source back to the caller,
     * to prevent changing it directly in the data store, but to keep transactional properties.
     * @param origEntity Original entity
     * @return
     */
    public V registerEntityForChanges(V origEntity) {
        final String key = origEntity.getId();
        // If the entity is listed in the transaction already, return it directly
        if (tasks.containsKey(key)) {
            MapTaskWithValue current = tasks.get(key);
            return current.getValue();
        }
        // Else enlist its copy in the transaction. Never return direct reference to the underlying map
        final V res = cloner.from(origEntity);
        return updateIfChanged(res, e -> e.isUpdated());
    }

    @Override
    public V read(String sKey) {
        try { 
            // TODO: Consider using Optional rather than handling NPE
            final V entity = read(sKey, map::read);
            return registerEntityForChanges(entity);
        } catch (NullPointerException ex) {
            return null;
        }
    }

    public V read(String key, Function<String, V> defaultValueFunc) {
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
     * @param queryParameters
     * @return
     */
    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> mcb = queryParameters.getModelCriteriaBuilder();
        MapModelCriteriaBuilder<K,V,M> mapMcb = mcb.flashToModelCriteriaBuilder(createCriteriaBuilder());

        Predicate<? super V> filterOutAllBulkDeletedObjects = tasks.values().stream()
          .filter(BulkDeleteOperation.class::isInstance)
          .map(BulkDeleteOperation.class::cast)
          .map(BulkDeleteOperation::getFilterForNonDeletedObjects)
          .reduce(Predicate::and)
          .orElse(v -> true);

        Stream<V> updatedAndNotRemovedObjectsStream = this.map.read(queryParameters)
          .filter(filterOutAllBulkDeletedObjects)
          .map(this::getUpdated)      // If the object has been removed, tx.get will return null, otherwise it will return me.getValue()
          .filter(Objects::nonNull)
          .map(this::registerEntityForChanges);

        // In case of created values stored in MapKeycloakTransaction, we need filter those according to the filter
        Stream<V> res = mapMcb == null
          ? updatedAndNotRemovedObjectsStream
          : Stream.concat(
              createdValuesStream(mapMcb.getKeyFilter(), mapMcb.getEntityFilter()),
              updatedAndNotRemovedObjectsStream
            );

        if (!queryParameters.getOrderBy().isEmpty()) {
            res = res.sorted(MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream()));
        }


        return res;
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return read(queryParameters).count();
    }

    private V getUpdated(V orig) {
        MapTaskWithValue current = orig == null ? null : tasks.get(orig.getId());
        return current == null ? orig : current.getValue();
    }

    @Override
    public V create(V value) {
        String key = value.getId();
        if (key == null) {
            K newKey = keyConverter.yieldNewUniqueKey();
            key = keyConverter.keyToString(newKey);
            value = cloner.from(key, value);
        } else {
            value = cloner.from(value);
        }
        addTask(key, new CreateOperation(value));
        return value;
    }

    public V updateIfChanged(V value, Predicate<V> shouldPut) {
        String key = value.getId();
        log.tracef("Adding operation UPDATE_IF_CHANGED for %s @ %08x", key, System.identityHashCode(value));

        String taskKey = key;
        MapTaskWithValue op = new MapTaskWithValue(value) {
            @Override
            public void execute() {
                if (shouldPut.test(getValue())) {
                    map.update(getValue());
                }
            }
            @Override public MapOperation getOperation() { return MapOperation.UPDATE; }
        };
        return tasks.merge(taskKey, op, this::merge).getValue();
    }

    @Override
    public boolean delete(String key) {
        tasks.merge(key, new DeleteOperation(key), this::merge);
        return true;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        log.tracef("Adding operation DELETE_BULK");

        K artificialKey = keyConverter.yieldNewUniqueKey();

        // Remove all tasks that create / update / delete objects deleted by the bulk removal.
        final BulkDeleteOperation bdo = new BulkDeleteOperation(queryParameters);
        Predicate<V> filterForNonDeletedObjects = bdo.getFilterForNonDeletedObjects();
        long res = 0;
        for (Iterator<Entry<String, MapTaskWithValue>> it = tasks.entrySet().iterator(); it.hasNext();) {
            Entry<String, MapTaskWithValue> me = it.next();
            if (! filterForNonDeletedObjects.test(me.getValue().getValue())) {
                log.tracef(" [DELETE_BULK] removing %s", me.getKey());
                it.remove();
                res++;
            }
        }

        tasks.put(keyConverter.keyToString(artificialKey), bdo);

        return res + bdo.getCount();
    }

    private Stream<V> createdValuesStream(Predicate<? super K> keyFilter, Predicate<? super V> entityFilter) {
        return this.tasks.entrySet().stream()
          .filter(me -> keyFilter.test(keyConverter.fromStringSafe(me.getKey())))
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
        public CreateOperation(V value) {
            super(value);
        }

        @Override public void execute() { map.create(getValue()); }
        @Override public MapOperation getOperation() { return MapOperation.CREATE; }
    }

    private class DeleteOperation extends MapTaskWithValue {
        private final String key;

        public DeleteOperation(String key) {
            super(null);
            this.key = key;
        }

        @Override public void execute() { map.delete(key); }
        @Override public MapOperation getOperation() { return MapOperation.DELETE; }
    }

    private class BulkDeleteOperation extends MapTaskWithValue {

        private final QueryParameters<M> queryParameters;

        public BulkDeleteOperation(QueryParameters<M> queryParameters) {
            super(null);
            this.queryParameters = queryParameters;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void execute() {
            map.delete(queryParameters);
        }

        public Predicate<V> getFilterForNonDeletedObjects() {
            DefaultModelCriteria<M> mcb = queryParameters.getModelCriteriaBuilder();
            MapModelCriteriaBuilder<K,V,M> mmcb = mcb.flashToModelCriteriaBuilder(createCriteriaBuilder());
            
            Predicate<? super V> entityFilter = mmcb.getEntityFilter();
            Predicate<? super K> keyFilter = mmcb.getKeyFilter();
            return v -> v == null || ! (keyFilter.test(keyConverter.fromStringSafe(v.getId())) && entityFilter.test(v));
        }

        @Override
        public MapOperation getOperation() {
            return MapOperation.DELETE;
        }

        private long getCount() {
            return map.getCount(queryParameters);
        }
    }
}
