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
import org.keycloak.models.map.storage.hotRod.transaction.NoActionHotRodTransactionWrapper;
import org.keycloak.models.map.storage.hotRod.transaction.AllAreasHotRodTransactionsWrapper;
import org.keycloak.storage.SearchableModelField;

import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    private final AllAreasHotRodTransactionsWrapper txWrapper;

    public HotRodMapStorage(RemoteCache<K, E> remoteCache, StringKeyConverter<K> keyConverter, HotRodEntityDescriptor<E, V> storedEntityDescriptor, DeepCloner cloner, AllAreasHotRodTransactionsWrapper txWrapper) {
        this.remoteCache = remoteCache;
        this.keyConverter = keyConverter;
        this.storedEntityDescriptor = storedEntityDescriptor;
        this.cloner = cloner;
        this.delegateProducer = storedEntityDescriptor.getHotRodDelegateProvider();
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(ModelEntityUtil.getEntityType(storedEntityDescriptor.getModelTypeClass()));
        this.txWrapper = txWrapper;
    }

    @Override
    public V create(V value) {
        K key = keyConverter.fromStringSafe(value.getId());
        if (key == null) {
            key = keyConverter.yieldNewUniqueKey();
            value = cloner.from(keyConverter.keyToString(key), value);
        }

        if (isExpirableEntity) {
            Long lifespan = getLifespan(value);
            if (lifespan != null) {
                if (lifespan > 0) {
                    remoteCache.putIfAbsent(key, value.getHotRodEntity(), lifespan, TimeUnit.MILLISECONDS);
                } else {
                    LOG.warnf("Skipped creation of entity %s in storage due to negative/zero lifespan.", key);
                }
                return value;
            }
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
        return delegateProducer.apply(hotRodEntity);
    }

    @Override
    public V update(V value) {
        K key = keyConverter.fromStringSafe(value.getId());

        if (isExpirableEntity) {
            Long lifespan = getLifespan(value);
            if (lifespan != null) {
                E previousValue;
                if (lifespan > 0) {
                    previousValue = remoteCache.replace(key, value.getHotRodEntity(), lifespan, TimeUnit.MILLISECONDS);
                } else {
                    LOG.warnf("Removing entity %s from storage due to negative/zero lifespan.", key);
                    previousValue = remoteCache.remove(key);
                }
                return previousValue == null ? null : delegateProducer.apply(previousValue);
            }
        }
        E previousValue = remoteCache.replace(key, value.getHotRodEntity());
        return previousValue == null ? null : delegateProducer.apply(previousValue);
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

    @Override
    public Stream<V> read(QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<E, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = iqmcb.getIckleQuery();

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
        String queryString = "DELETE " + iqmcb.getIckleQuery();

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                    .collect(Collectors.joining(", "));
        }

        LOG.tracef("Executing delete Ickle query: %s", queryString);

        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);

        if (queryParameters.getLimit() != null || queryParameters.getOffset() != null) {
            throw new IllegalArgumentException("HotRod storage does not support pagination for delete query");
        }
        Query<Object[]> query = queryFactory.create(queryString);

        query.setParameters(iqmcb.getParameters());

        return query.executeStatement();
    }

    @Override
    public boolean exists(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConverter.fromStringSafe(key);

        return remoteCache.containsKey(k);
    }

    public IckleQueryMapModelCriteriaBuilder<E, M> createCriteriaBuilder() {
        return new IckleQueryMapModelCriteriaBuilder<>(storedEntityDescriptor.getEntityTypeClass());
    }

    @Override
    public MapKeycloakTransaction<V, M> createTransaction(KeycloakSession session) {
        // Here we return transaction that has no action because the returned transaction is enlisted to different
        //  phase than we need. Instead of tx returned by this method txWrapper is enlisted and executes all changes
        //  performed by the returned transaction.
        return new NoActionHotRodTransactionWrapper<>((ConcurrentHashMapKeycloakTransaction<K, V, M>) txWrapper.getOrCreateTxForModel(storedEntityDescriptor.getModelTypeClass(), () -> createTransactionInternal(session)));
    }

    protected MapKeycloakTransaction<V, M> createTransactionInternal(KeycloakSession session) {
        Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, V, M>> fieldPredicates = MapFieldPredicates.getPredicates((Class<M>) storedEntityDescriptor.getModelTypeClass());
        return new ConcurrentHashMapKeycloakTransaction<>(this, keyConverter, cloner, fieldPredicates);
    }

    // V must be an instance of ExpirableEntity
    // returns null if expiration field is not set
    // in certain cases can return 0 or negative number, which needs to be handled carefully when using as ISPN lifespan
    private Long getLifespan(V value) {
        Long expiration = ((ExpirableEntity) value).getExpiration();
        return expiration != null ? expiration - Time.currentTimeMillis() : null;
    }
}
