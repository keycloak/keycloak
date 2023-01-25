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
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelEntityUtil;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapCrudOperations;
import org.keycloak.models.map.storage.chm.ConcurrentHashMapKeycloakTransaction;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDelegate;
import org.keycloak.models.map.storage.hotRod.common.HotRodEntityDescriptor;
import org.keycloak.models.map.storage.hotRod.connections.DefaultHotRodConnectionProviderFactory;
import org.keycloak.models.map.storage.hotRod.connections.HotRodConnectionProvider;
import org.keycloak.models.map.storage.hotRod.locking.HotRodLocksUtils;
import org.keycloak.models.map.storage.hotRod.transaction.AllAreasHotRodTransactionsWrapper;
import org.keycloak.models.map.storage.hotRod.transaction.NoActionHotRodTransactionWrapper;
import org.keycloak.storage.SearchableModelField;
import org.keycloak.utils.LockObjectsForModification;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.hotRod.common.HotRodUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class HotRodMapStorage<K, E extends AbstractHotRodEntity, V extends AbstractEntity & HotRodEntityDelegate<E>, M> implements MapStorage<V, M>, ConcurrentHashMapCrudOperations<V, M> {

    private static final Logger LOG = Logger.getLogger(HotRodMapStorage.class);

    private final KeycloakSession session;
    private final RemoteCache<K, E> remoteCache;
    protected final StringKeyConverter<K> keyConverter;
    protected final HotRodEntityDescriptor<E, V> storedEntityDescriptor;
    private final Function<E, V> delegateProducer;
    protected final DeepCloner cloner;
    protected boolean isExpirableEntity;
    private final AllAreasHotRodTransactionsWrapper txWrapper;
    private final Map<SearchableModelField<? super M>, MapModelCriteriaBuilder.UpdatePredicatesFunc<K, V, M>> fieldPredicates;
    private final Long lockTimeout;
    private final RemoteCache<String, String> locksCache;

    public HotRodMapStorage(KeycloakSession session, RemoteCache<K, E> remoteCache, StringKeyConverter<K> keyConverter, HotRodEntityDescriptor<E, V> storedEntityDescriptor, DeepCloner cloner, AllAreasHotRodTransactionsWrapper txWrapper, Long lockTimeout) {
        this.session = session;
        this.remoteCache = remoteCache;
        this.keyConverter = keyConverter;
        this.storedEntityDescriptor = storedEntityDescriptor;
        this.cloner = cloner;
        this.delegateProducer = storedEntityDescriptor.getHotRodDelegateProvider();
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(ModelEntityUtil.getEntityType(storedEntityDescriptor.getModelTypeClass()));
        this.txWrapper = txWrapper;
        this.fieldPredicates = MapFieldPredicates.getPredicates((Class<M>) storedEntityDescriptor.getModelTypeClass());
        this.lockTimeout = lockTimeout;
        HotRodConnectionProvider cacheProvider = session.getProvider(HotRodConnectionProvider.class);
        this.locksCache = cacheProvider.getRemoteCache(DefaultHotRodConnectionProviderFactory.HOT_ROD_LOCKS_CACHE_NAME);
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

    private String getLockName(String key) {
        return storedEntityDescriptor.getModelTypeClass().getName() + "_" + key;
    }

    @Override
    public V read(String key) {
        Objects.requireNonNull(key, "Key must be non-null");
        K k = keyConverter.fromStringSafe(key);

        if (LockObjectsForModification.isEnabled(session, storedEntityDescriptor.getModelTypeClass())) {
            String lockName = getLockName(key);
            HotRodLocksUtils.repeatPutIfAbsent(locksCache, lockName, Duration.ofMillis(lockTimeout), 50, true);

            session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
                @Override
                protected void commitImpl() {
                    HotRodLocksUtils.removeWithInstanceIdentifier(locksCache, lockName);
                }

                @Override
                protected void rollbackImpl() {
                    HotRodLocksUtils.removeWithInstanceIdentifier(locksCache, lockName);
                }
            });
        }

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
        if (LockObjectsForModification.isEnabled(session, storedEntityDescriptor.getModelTypeClass())) {
            return pessimisticQueryRead(queryParameters);
        }

        Query<E> query = prepareQueryWithPrefixAndParameters(null, queryParameters);
        CloseableIterator<E> iterator = paginateQuery(query, queryParameters.getOffset(),
                queryParameters.getLimit()).iterator();
        return closing(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false))
                .onClose(iterator::close)
                .filter(Objects::nonNull) // see https://github.com/keycloak/keycloak/issues/9271
                .map(this.delegateProducer);
    }

    private Stream<V> pessimisticQueryRead(QueryParameters<M> queryParameters) {
        DefaultModelCriteria<M> dmc = queryParameters.getModelCriteriaBuilder();

        // Optimization if the criteria contains only one id
        String id = (String) dmc.getSingleRestrictionArgument("id");

        if (id != null) {
            // We have a criteria that contains "id EQ 'some_key'". We can change this to reading only some_key using read method and then apply the rest of criteria.
            MapModelCriteriaBuilder<K,V,M> mapMcb = dmc.flashToModelCriteriaBuilder(new MapModelCriteriaBuilder<>(keyConverter, fieldPredicates));
            V entity = read(id);
            if (entity == null) {
                return Stream.empty();
            }
            boolean fulfillsQueryCriteria = mapMcb.getKeyFilter().test(keyConverter.fromString(id)) && mapMcb.getEntityFilter().test(entity);
            if (!fulfillsQueryCriteria) {
                // entity does not fulfill whole criteria, we can release lock now
                HotRodLocksUtils.removeWithInstanceIdentifier(locksCache, getLockName(id));
                return Stream.empty();
            }

            return Stream.of(entity);
        }

        // Criteria does not contain only one id, we need to read ids non-pessimistically and then read entities one by one pessimistically
        Query<Object[]> query = prepareQueryWithPrefixAndParameters("SELECT id ", queryParameters);
        CloseableIterator<Object[]> iterator = paginateQuery(query, queryParameters.getOffset(),
                queryParameters.getLimit()).iterator();

        return closing(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false))
                .onClose(iterator::close)
                // Extract ids from the result
                .map(a -> a[0])
                .map(String.class::cast)
                // Pessimistically read
                .map(this::read)
                // Entity can be removed in the meanwhile, we need to check for null
                .filter(Objects::nonNull);

    }

    private <T> Query<T> prepareQueryWithPrefixAndParameters(String prefix, QueryParameters<M> queryParameters) {
        IckleQueryMapModelCriteriaBuilder<E, M> iqmcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createCriteriaBuilder());
        String queryString = (prefix != null ? prefix : "") + iqmcb.getIckleQuery();

        if (!queryParameters.getOrderBy().isEmpty()) {
            queryString += " ORDER BY " + queryParameters.getOrderBy().stream().map(HotRodMapStorage::toOrderString)
                    .collect(Collectors.joining(", "));
        }
        LOG.tracef("Preparing Ickle query: '%s'%s", queryString, getShortStackTrace());
        QueryFactory queryFactory = Search.getQueryFactory(remoteCache);
        Query<T> query = queryFactory.create(queryString);

        query.setParameters(iqmcb.getParameters());
        return query;
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
        if (queryParameters.getLimit() != null || queryParameters.getOffset() != null) {
            throw new IllegalArgumentException("HotRod storage does not support pagination for delete query");
        }

        Query<Object[]> query = prepareQueryWithPrefixAndParameters("DELETE ", queryParameters);
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
