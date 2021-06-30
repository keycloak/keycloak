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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.storage.SearchableModelField;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.keycloak.models.map.storage.StringKeyConvertor;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder.UpdatePredicatesFunc;
import java.util.Objects;
import java.util.function.Predicate;

import static org.keycloak.utils.StreamsUtil.paginatedStream;

/**
 *
 * @author hmlnarik
 */
public class ConcurrentHashMapStorage<K, V extends AbstractEntity<K>, M> implements MapStorage<K, V, M> {

    private final ConcurrentMap<K, V> store = new ConcurrentHashMap<>();

    private final Map<SearchableModelField<M>, UpdatePredicatesFunc<K, V, M>> fieldPredicates;
    private final StringKeyConvertor<K> keyConvertor;

    @SuppressWarnings("unchecked")
    public ConcurrentHashMapStorage(Class<M> modelClass, StringKeyConvertor<K> keyConvertor) {
        this.fieldPredicates = MapFieldPredicates.getPredicates(modelClass);
        this.keyConvertor = keyConvertor;
    }

    @Override
    public V create(V value) {
        K key = value.getId();
        return store.putIfAbsent(key, value);
    }

    @Override
    public V read(K key) {
        Objects.requireNonNull(key, "Key must be non-null");
        return store.get(key);
    }

    @Override
    public V update(V value) {
        K key = value.getId();
        return store.replace(key, value);
    }

    @Override
    public boolean delete(K key) {
        return store.remove(key) != null;
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        ModelCriteriaBuilder<M> criteria = queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            long res = store.size();
            store.clear();
            return res;
        }

        MapModelCriteriaBuilder<K, V, M> b = criteria.unwrap(MapModelCriteriaBuilder.class);
        if (b == null) {
            throw new IllegalStateException("Incompatible class: " + criteria.getClass());
        }
        Predicate<? super K> keyFilter = b.getKeyFilter();
        Predicate<? super V> entityFilter = b.getEntityFilter();
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
    public ModelCriteriaBuilder<M> createCriteriaBuilder() {
        return new MapModelCriteriaBuilder<>(fieldPredicates);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapKeycloakTransaction<K, V, M> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<K, V, M> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);
        return sessionTransaction == null ? new ConcurrentHashMapKeycloakTransaction<>(this) : sessionTransaction;
    }

    @Override
    public StringKeyConvertor<K> getKeyConvertor() {
        return keyConvertor;
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        ModelCriteriaBuilder<M> criteria = queryParameters.getModelCriteriaBuilder();

        if (criteria == null) {
            return Stream.empty();
        }
        Stream<Entry<K, V>> stream = store.entrySet().stream();

        MapModelCriteriaBuilder<K, V, M> b = criteria.unwrap(MapModelCriteriaBuilder.class);
        if (b == null) {
            throw new IllegalStateException("Incompatible class: " + criteria.getClass());
        }
        Predicate<? super K> keyFilter = b.getKeyFilter();
        Predicate<? super V> entityFilter = b.getEntityFilter();
        stream = stream.filter(me -> keyFilter.test(me.getKey()) && entityFilter.test(me.getValue()));

        return stream.map(Map.Entry::getValue);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        return read(queryParameters).count();
    }

}
