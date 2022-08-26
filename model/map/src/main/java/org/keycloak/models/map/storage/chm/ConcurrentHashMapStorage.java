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

import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.ExpirationUtils;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelEntityUtil;
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

import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
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
public class ConcurrentHashMapStorage<K, V extends AbstractEntity & UpdatableEntity, M> implements MapStorage<V, M>, ConcurrentHashMapCrudOperations<V, M> {

    protected final ConcurrentMap<K, V> store = new ConcurrentHashMap<>();

    protected final Map<SearchableModelField<? super M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;
    protected final StringKeyConverter<K> keyConverter;
    protected final DeepCloner cloner;
    private final boolean isExpirableEntity;

    @SuppressWarnings("unchecked")
    public ConcurrentHashMapStorage(Class<M> modelClass, StringKeyConverter<K> keyConverter, DeepCloner cloner) {
        this.fieldPredicates = MapFieldPredicates.getPredicates(modelClass);
        this.keyConverter = keyConverter;
        this.cloner = cloner;
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(ModelEntityUtil.getEntityType(modelClass));
    }

    @Override
    public V create(V value) {
        K key = keyConverter.fromStringSafe(value.getId());
        if (key == null) {
            key = keyConverter.yieldNewUniqueKey();
            value = cloner.from(keyConverter.keyToString(key), value);
        }
        store.putIfAbsent(key, value);
        return value;
    }

    @Override
    public V read(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConverter.fromStringSafe(key);

        V v = store.get(k);
        if (v == null) return null;
        return isExpirableEntity && isExpired((ExpirableEntity) v, true) ? null : v;
    }

    @Override
    public V update(V value) {
        K key = getKeyConverter().fromStringSafe(value.getId());
        return store.replace(key, value);
    }

    @Override
    public boolean delete(String key) {
        K k = getKeyConverter().fromStringSafe(key);
        return store.remove(k) != null;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            long res = store.size();
            store.clear();
            return res;
        }

        @SuppressWarnings("unchecked")
        MapModelCriteriaBuilder<K,V,M> mcb = criteria.flashToModelCriteriaBuilder(createCriteriaBuilder());
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

        if (sessionTransaction == null) {
            sessionTransaction = new ConcurrentHashMapKeycloakTransaction<>(this, keyConverter, cloner, fieldPredicates);
            session.setAttribute("map-transaction-" + hashCode(), sessionTransaction);
        }
        return sessionTransaction;
    }

    public MapModelCriteriaBuilder<K, V, M> createCriteriaBuilder() {
        return new MapModelCriteriaBuilder<>(keyConverter, fieldPredicates);
    }

    public StringKeyConverter<K> getKeyConverter() {
        return keyConverter;
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> criteria = queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            return Stream.empty();
        }

        MapModelCriteriaBuilder<K,V,M> mcb = criteria.flashToModelCriteriaBuilder(createCriteriaBuilder());
        Stream<Entry<K, V>> stream = store.entrySet().stream();

        Predicate<? super K> keyFilter = mcb.getKeyFilter();
        Predicate<? super V> entityFilter;

        if (isExpirableEntity) {
            entityFilter = mcb.getEntityFilter().and(ExpirationUtils::isNotExpired);
        } else {
            entityFilter = mcb.getEntityFilter();
        }

        Stream<V> valueStream = stream.filter(me -> keyFilter.test(me.getKey()) && entityFilter.test(me.getValue()))
                .map(Map.Entry::getValue);

        if (!queryParameters.getOrderBy().isEmpty()) {
            valueStream = valueStream.sorted(MapFieldPredicates.getComparator(queryParameters.getOrderBy().stream()));
        }

        return paginatedStream(valueStream, queryParameters.getOffset(), queryParameters.getLimit());
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return read(queryParameters).count();
    }

}
