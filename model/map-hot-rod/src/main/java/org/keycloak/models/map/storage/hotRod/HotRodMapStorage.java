/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.HotRodEntityDescriptor;
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.keycloak.models.map.common.HotRodUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class HotRodMapStorage<K, V extends AbstractEntity & UpdatableEntity, M> implements MapStorage<V, M>, ConcurrentHashMapCrudOperations<V, M> {

    private static final Logger LOG = Logger.getLogger(HotRodMapStorage.class);

    private final RemoteCache<K, V> remoteCache;
    private final StringKeyConvertor<K> keyConvertor;
    private final HotRodEntityDescriptor<V> storedEntityDescriptor;
    private final DeepCloner cloner;

    public HotRodMapStorage(RemoteCache<K, V> remoteCache, StringKeyConvertor<K> keyConvertor, HotRodEntityDescriptor<V> storedEntityDescriptor, DeepCloner cloner) {
        this.remoteCache = remoteCache;
        this.keyConvertor = keyConvertor;
        this.storedEntityDescriptor = storedEntityDescriptor;
        this.cloner = cloner;
    }

    @Override
    public V create(V value) {
        K key = keyConvertor.fromStringSafe(value.getId());
        if (key == null) {
            key = keyConvertor.yieldNewUniqueKey();
            value = cloner.from(keyConvertor.keyToString(key), value);
        }

        remoteCache.putIfAbsent(key, value);

        return value;
    }

    @Override
    public V read(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConvertor.fromStringSafe(key);
        return remoteCache.get(k);
    }

    @Override
    public V update(V value) {
        K key = keyConvertor.fromStringSafe(value.getId());
        return remoteCache.replace(key, value);
    }

    @Override
    public boolean delete(String key) {
        K k = keyConvertor.fromStringSafe(key);
        return remoteCache.remove(k) != null;
    }

    private static String toOrderString(QueryParameters.OrderBy<?> orderBy) {
        SearchableModelField<?> field = orderBy.getModelField();
        String modelFieldName = IckleQueryMapModelCriteriaBuilder.getFieldName(field);
        String orderString = orderBy.getOrder().equals(QueryParameters.Order.ASCENDING) ? "ASC" : "DESC";

        return modelFieldName + " " + orderString;
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<K, V, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = iqmcb.getIckleQuery();

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                                            .collect(Collectors.joining(", "));
        }

        LOG.tracef("Executing read Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<V> query = paginateQuery(queryFactory.create(queryString), queryParameters.getOffset(),
                queryParameters.getLimit());

        query.setParameters(iqmcb.getParameters());

        CloseableIterator<V> iterator = query.iterator();
        return closing(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false))
                .onClose(iterator::close);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<K, V, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = iqmcb.getIckleQuery();

        LOG.tracef("Executing count Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<V> query = queryFactory.create(queryString);
        query.setParameters(iqmcb.getParameters());

        return query.execute().hitCount().orElse(0);
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<K, V, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = "SELECT id " + iqmcb.getIckleQuery();

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                    .collect(Collectors.joining(", "));
        }

        LOG.tracef("Executing delete Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<V> query = paginateQuery(queryFactory.create(queryString), queryParameters.getOffset(),
                queryParameters.getLimit());

        query.setParameters(iqmcb.getParameters());

        AtomicLong result = new AtomicLong();

        CloseableIterator<V> iterator = query.iterator();
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .peek(e -> result.incrementAndGet())
                .map(AbstractEntity::getId)
                .forEach(this::delete);
        iterator.close();

        return result.get();
    }

    public IckleQueryMapModelCriteriaBuilder<K, V, M> createCriteriaBuilder() {
        return new IckleQueryMapModelCriteriaBuilder<>();
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, V, M>> fieldPredicates = MapFieldPredicates.getPredicates((Class<M>) storedEntityDescriptor.getModelTypeClass());
        return new ConcurrentHashMapKeycloakTransaction<>(this, keyConvertor, cloner, fieldPredicates);
    }
}
