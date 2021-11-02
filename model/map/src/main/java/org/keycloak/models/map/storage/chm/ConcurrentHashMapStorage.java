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

import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.storage.SearchableModelField;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import java.util.Objects;
import java.util.function.Predicate;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 *
 * It contains basic object CRUD operations as well as bulk {@link #read(org.keycloak.models.map.storage.QueryParameters)}
 * and bulk {@link #delete(org.keycloak.models.map.storage.QueryParameters)} operations,
 * and operation for determining the number of the objects satisfying given criteria
 * ({@link #getCount(org.keycloak.models.map.storage.QueryParameters)}).
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorage<K, V extends AbstractEntity & UpdatableEntity, M> implements MapStorage<V, M> {

    protected final ConcurrentMap<K, V> store = new ConcurrentHashMap<>();

    protected final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;
    protected final StringKeyConvertor<K> keyConvertor;
    protected final DeepCloner cloner;

    @SuppressWarnings("unchecked")
    public ConcurrentHashMapStorage(Class<M> modelClass, StringKeyConvertor<K> keyConvertor, DeepCloner cloner) {
        this.fieldPredicates = MapFieldPredicates.getPredicates(modelClass);
        this.keyConvertor = keyConvertor;
        this.cloner = cloner;
    }

    /**
     * Creates an object in the store. ID of the {@code value} may be prescribed in id of the {@code value}.
     * If the id is {@code null} or its format is not matching the store internal format for ID, then
     * the {@code value}'s ID will be generated and returned in the id of the return value.
     * @param value Entity to create in the store
     * @throws NullPointerException if {@code value} is {@code null}
     * @see AbstractEntity#getId()
     * @return Entity representing the {@code value} in the store. It may or may not be the same instance as {@code value}
     */
    public V create(V value) {
        K key = keyConvertor.fromStringSafe(value.getId());
        if (key == null) {
            key = keyConvertor.yieldNewUniqueKey();
            value = cloner.from(keyConvertor.keyToString(key), value);
        }
        store.putIfAbsent(key, value);
        return value;
    }

    /**
     * Returns object with the given {@code key} from the storage or {@code null} if object does not exist.
     * <br>
     * TODO: Consider returning {@code Optional<V>} instead.
     * @param key Key of the object. Must not be {@code null}.
     * @return See description
     * @throws NullPointerException if the {@code key} is {@code null}
     */
    public V read(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConvertor.fromStringSafe(key);
        return store.get(k);
    }

    /**
     * Updates the object with the key of the {@code value}'s ID in the storage if it already exists.
     *
     * @param value Updated value
     * @throws NullPointerException if the object or its {@code id} is {@code null}
     * @see AbstractEntity#getId()
     */
    public V update(V value) {
        K key = getKeyConvertor().fromStringSafe(value.getId());
        return store.replace(key, value);
    }

    /**
     * Deletes object with the given {@code key} from the storage, if exists, no-op otherwise.
     * @param key
     * @return Returns {@code true} if the object has been deleted or result cannot be determined, {@code false} otherwise.
     */
    public boolean delete(String key) {
        K k = getKeyConvertor().fromStringSafe(key);
        return store.remove(k) != null;
    }

    /**
     * Deletes objects that match the given criteria.
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Number of removed objects (might return {@code -1} if not supported)
     */
    public long delete(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = (DefaultModelCriteria<M>) queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            long res = store.size();
            store.clear();
            return res;
        }

        @SuppressWarnings("unchecked")
        MapModelCriteriaBuilder<K,V,M> mcb = (MapModelCriteriaBuilder<K,V,M>) criteria.flashToModelCriteriaBuilder(createCriteriaBuilder());
        Predicate<? super K> keyFilter = mcb.getKeyFilter();
        Predicate<? super V> entityFilter = mcb.getEntityFilter();
        Stream<Entry<K, V>> storeStream = store.entrySet().stream();
        final AtomicLong res = new AtomicLong(0);

        if (!queryParameters.getOrderBy().isEmpty()) {
            Comparator<V> comparator = MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream());
            storeStream = storeStream.sorted((entry1, entry2) -> comparator.compare(entry1.getValue(), entry2.getValue()));
        }

        paginatedStream(storeStream.filter(next -> keyFilter.test(next.getKey()) && entityFilter.test(next.getValue()))
                , queryParameters.getOffset(), queryParameters.getLimit())
                .peek(item -> {res.incrementAndGet();})
                .map(Entry::getKey)
                .forEach(store::remove);

        return res.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<V, M> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);
        return sessionTransaction == null ? new ConcurrentHashMapKeycloakTransaction<>(this, keyConvertor, cloner) : sessionTransaction;
    }

    public ModelCriteriaBuilder<M> createCriteriaBuilder() {
        return new MapModelCriteriaBuilder<>(keyConvertor, fieldPredicates);
    }

    public StringKeyConvertor<K> getKeyConvertor() {
        return keyConvertor;
    }

    /**
     * Returns stream of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Stream of objects. Never returns {@code null}.
     */
    public Stream<V> read(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = (DefaultModelCriteria<M>) queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            return Stream.empty();
        }

        MapModelCriteriaBuilder<K,V,M> mcb = (MapModelCriteriaBuilder<K,V,M>) criteria.flashToModelCriteriaBuilder(createCriteriaBuilder());
        Stream<Entry<K, V>> stream = store.entrySet().stream();

        Predicate<? super K> keyFilter = mcb.getKeyFilter();
        Predicate<? super V> entityFilter = mcb.getEntityFilter();
        stream = stream.filter(me -> keyFilter.test(me.getKey()) && entityFilter.test(me.getValue()));

        return stream.map(Map.Entry::getValue);
    }

    /**
     * Returns the number of objects satisfying given {@code criteria} from the storage.
     * The criteria are specified in the given criteria builder based on model properties.
     *
     * @param queryParameters parameters for the query like firstResult, maxResult, requested ordering, etc.
     * @return Number of objects. Never returns {@code null}.
     */
    public long getCount(QueryParameters<M> queryParameters) {
        return read(queryParameters).count();
    }

}
