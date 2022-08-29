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
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.common.StringKeyConverter;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
import static org.keycloak.models.map.storage.hotRod.common.HotRodUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class HotRodMapStorage<K, E extends AbstractHotRodEntity, V extends AbstractEntity & HotRodEntityDelegate<E>, M> implements MapStorage<V, M>, ConcurrentHashMapCrudOperations<V, M> {

    private static final Logger LOG = Logger.getLogger(HotRodMapStorage.class);

    private final RemoteCache<K, E> remoteCache;
    protected final StringKeyConverter<K> keyConverter;
    protected final HotRodEntityDescriptor<E, V> storedEntityDescriptor;
    private final Function<E, V> delegateProducer;
    protected final DeepCloner cloner;
    protected boolean isExpirableEntity;

    public HotRodMapStorage(RemoteCache<K, E> remoteCache, StringKeyConverter<K> keyConverter, HotRodEntityDescriptor<E, V> storedEntityDescriptor, DeepCloner cloner) {
        this.remoteCache = remoteCache;
        this.keyConverter = keyConverter;
        this.storedEntityDescriptor = storedEntityDescriptor;
        this.cloner = cloner;
        this.delegateProducer = storedEntityDescriptor.getHotRodDelegateProvider();
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(ModelEntityUtil.getEntityType(storedEntityDescriptor.getModelTypeClass()));
    }

    @Override
    public V create(V value) {
        K key = keyConverter.fromStringSafe(value.getId());
        if (key == null) {
            key = keyConverter.yieldNewUniqueKey();
            value = cloner.from(keyConverter.keyToString(key), value);
        }

        remoteCache.putIfAbsent(key, value.getHotRodEntity());

        return value;
    }

    @Override
    public V read(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConverter.fromStringSafe(key);

        // Obtain value from Infinispan
        E hotRodEntity = remoteCache.get(k);
        if (hotRodEntity == null) return null;

        // Create delegate that implements Map*Entity
        V delegateEntity = delegateProducer.apply(hotRodEntity);

        // Check expiration if necessary and return value
        return isExpirableEntity && isExpired((ExpirableEntity) delegateEntity, true) ? null : delegateEntity;
    }

    @Override
    public V update(V value) {
        K key = keyConverter.fromStringSafe(value.getId());

        E previousValue = remoteCache.replace(key, value.getHotRodEntity());
        if (previousValue == null) return null;

        return delegateProducer.apply(previousValue);
    }

    @Override
    public boolean delete(String key) {
        K k = keyConverter.fromStringSafe(key);
        return remoteCache.remove(k) != null;
    }

    private static String toOrderString(QueryParameters.OrderBy<?> orderBy) {
        SearchableModelField<?> field = orderBy.getModelField();
        String modelFieldName = IckleQueryMapModelCriteriaBuilder.getFieldName(field);
        String orderString = orderBy.getOrder().equals(QueryParameters.Order.ASCENDING) ? "ASC" : "DESC";

        return modelFieldName + " " + orderString;
    }

    private static String isNotExpiredIckleWhereClause() {
        return "(" + IckleQueryOperators.C + ".expiration > " + Time.currentTimeMillis() + " OR "
                + IckleQueryOperators.C + ".expiration is null)";
    }

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<E, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = iqmcb.getIckleQuery();

        // Temporary solution until https://github.com/keycloak/keycloak/issues/12068 is fixed
        if (isExpirableEntity) {
            queryString += (queryString.contains("WHERE") ? " AND " : " WHERE ") + isNotExpiredIckleWhereClause();
        }

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                                            .collect(Collectors.joining(", "));
        }

        LOG.tracef("Executing read Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<E> query = paginateQuery(queryFactory.create(queryString), queryParameters.getOffset(),
                queryParameters.getLimit());

        query.setParameters(iqmcb.getParameters());

        CloseableIterator<E> iterator = query.iterator();
        return closing(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false))
                .onClose(iterator::close)
                .filter(Objects::nonNull) // see https://github.com/keycloak/keycloak/issues/9271
                .map(this.delegateProducer);
    }

    @Override
    public long getCount(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<E, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = iqmcb.getIckleQuery();

        LOG.tracef("Executing count Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<E> query = queryFactory.create(queryString);
        query.setParameters(iqmcb.getParameters());

        return query.execute().hitCount().orElse(0);
    }

    @Override
    public long delete(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<E, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = "SELECT id " + iqmcb.getIckleQuery();

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                    .collect(Collectors.joining(", "));
        }

        LOG.tracef("Executing delete Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        Query<Object[]> query = paginateQuery(queryFactory.create(queryString), queryParameters.getOffset(),
                queryParameters.getLimit());

        query.setParameters(iqmcb.getParameters());

        AtomicLong result = new AtomicLong();

        CloseableIterator<Object[]> iterator = query.iterator();
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .peek(e -> result.incrementAndGet())
                .map(a -> a[0])
                .map(String.class::cast)
                .forEach(this::delete);
        iterator.close();

        return result.get();
    }

    public IckleQueryMapModelCriteriaBuilder<E, M> createCriteriaBuilder() {
        return new IckleQueryMapModelCriteriaBuilder<>(storedEntityDescriptor.getEntityTypeClass());
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        MapKeycloakTransaction<V, M> sessionTransaction = session.getAttribute("map-transaction-" + hashCode(), MapKeycloakTransaction.class);

        if (sessionTransaction == null) {
            sessionTransaction = createTransactionInternal(session);
            session.setAttribute("map-transaction-" + hashCode(), sessionTransaction);
        }
        return sessionTransaction;
    }

    protected MapKeycloakTransaction<V, M> createTransactionInternal(KeycloakSession session) {
        Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, V, M>> fieldPredicates = MapFieldPredicates.getPredicates((Class<M>) storedEntityDescriptor.getModelTypeClass());
        return new ConcurrentHashMapKeycloakTransaction<>(this, keyConverter, cloner, fieldPredicates);
    }
}
