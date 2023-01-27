/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import org.keycloak.models.map.storage.chm.MapFieldPredicates;
import org.keycloak.models.map.storage.chm.MapModelCriteriaBuilder;
import org.keycloak.utils.LockObjectsForModification;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;
import static org.keycloak.models.map.storage.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public abstract class JpaMapKeycloakTransaction<RE extends JpaRootEntity, E extends AbstractEntity, M> implements MapKeycloakTransaction<E, M> {

    private static final Logger logger = Logger.getLogger(JpaMapKeycloakTransaction.class);
    private final KeycloakSession session;
    private final Class<RE> entityType;
    private final Class<M> modelType;
    private final boolean isExpirableEntity;
    protected EntityManager em;

    public JpaMapKeycloakTransaction(KeycloakSession session, Class<RE> entityType, Class<M> modelType, EntityManager em) {
        this.session = session;
        this.em = em;
        this.entityType = entityType;
        this.modelType = modelType;
        this.isExpirableEntity = ExpirableEntity.class.isAssignableFrom(entityType);
    }

    protected abstract Selection<? extends RE> selectCbConstruct(CriteriaBuilder cb, Root<RE> root);
    protected abstract void setEntityVersion(JpaRootEntity entity);
    protected abstract JpaModelCriteriaBuilder createJpaModelCriteriaBuilder();
    protected abstract E mapToEntityDelegate(RE original);

    private final HashMap<String, E> cacheWithinSession = new HashMap<>();

    /**
     * Use the cache within the session to ensure that there is only one instance per entity within the current session.
     */
    protected E mapToEntityDelegateUnique(RE original) {
        if (original == null) {
            return null;
        }
        E entity = cacheWithinSession.get(original.getId());
        if (entity == null) {
            entity = mapToEntityDelegate(original);
            cacheWithinSession.put(original.getId(), entity);
        }
        return entity;
    }

    @Override
    public E create(E mapEntity) {
        RE jpaEntity = entityType.cast(CLONER.from(mapEntity));
        if (mapEntity.getId() == null) {
            jpaEntity.setId(StringKeyConverter.UUIDKey.INSTANCE.yieldNewUniqueKey().toString());
        }
        logger.tracef("tx %d: create entity %s", hashCode(), jpaEntity.getId());
        setEntityVersion(jpaEntity);
        em.persist(jpaEntity);
        return mapToEntityDelegateUnique(jpaEntity);
    }

    @Override
    public E read(String key) {
        if (key == null) return null;
        E e = null;
        if (!LockObjectsForModification.isEnabled(session, modelType)) {
            e = cacheWithinSession.get(key);
        }
        if (e == null) {
            UUID uuid = StringKeyConverter.UUIDKey.INSTANCE.fromStringSafe(key);
            if (uuid == null) return null;
            e = mapToEntityDelegateUnique(
                    LockObjectsForModification.isEnabled(session, modelType) ?
                            em.find(entityType, uuid, LockModeType.PESSIMISTIC_WRITE) :
                            em.find(entityType, uuid)
            );
        }
        return e != null && isExpirableEntity && isExpired((ExpirableEntity) e, true) ? null : e;
    }

    private final static String JPA_MAP_CACHE = "keycloak.jpamap.cache";

    @Override
    @SuppressWarnings("unchecked")
    public Stream<E> read(QueryParameters<M> queryParameters) {
        Map<QueryCacheKey, List<RE>> cache = getQueryCache();
        QueryCacheKey queryCacheKey = new QueryCacheKey(queryParameters, modelType);
        if (!LockObjectsForModification.isEnabled(this.session, modelType)) {
            List<RE> previousResult = cache.get(queryCacheKey);
            SessionImpl session = (SessionImpl) em.unwrap(Session.class);
            // only do dirty checking if there is a previously cached result that would match the query
            if (previousResult != null) {
                // if the session is dirty, data has been modified, and the cache must not be used
                // check if there are queued actions already, as this allows us to skip the expensive dirty check
                if (!session.getActionQueue().areInsertionsOrDeletionsQueued() && session.getActionQueue().numberOfUpdates() == 0 && session.getActionQueue().numberOfCollectionUpdates() == 0 &&
                        !session.isDirty()) {
                    logger.tracef("tx %d: cache hit for %s/%s%s", hashCode(), queryParameters, queryCacheKey, getShortStackTrace());
                    return closing(previousResult.stream()).map(this::mapToEntityDelegateUnique);
                } else {
                    logger.tracef("tx %d: cache ignored due to dirty session", hashCode());
                }
            }
        }
        logger.tracef("tx %d: cache miss for %s/%s%s", hashCode(), queryParameters, queryCacheKey, getShortStackTrace());

        JpaModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createJpaModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RE> query = cb.createQuery(entityType);
        Root<RE> root = query.from(entityType);
        query.select(selectCbConstruct(cb, root));
        if (mcb.isDistinct()) query.distinct(true);

        //ordering
        if (!queryParameters.getOrderBy().isEmpty()) {
            List<Order> orderByList = new LinkedList<>();
            for (QueryParameters.OrderBy<M> order : queryParameters.getOrderBy()) {
                switch (order.getOrder()) {
                    case ASCENDING:
                        orderByList.add(cb.asc(root.get(order.getModelField().getName())));
                        break;
                    case DESCENDING:
                        orderByList.add(cb.desc(root.get(order.getModelField().getName())));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown ordering.");
                }
            }
            query.orderBy(orderByList);
        }

        JpaPredicateFunction<RE> predicateFunc = mcb.getPredicateFunc();
        if (this.isExpirableEntity) {
            predicateFunc = predicateFunc != null ? predicateFunc.andThen(predicate -> cb.and(predicate, notExpired(cb, query::subquery, root)))
                                                  : this::notExpired;
        }
        if (predicateFunc != null) query.where(predicateFunc.apply(cb, query::subquery, root));

        TypedQuery<RE> emQuery = paginateQuery(em.createQuery(query), queryParameters.getOffset(), queryParameters.getLimit());

        if (LockObjectsForModification.isEnabled(session, modelType)) {
            emQuery = emQuery.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }

        try {
            // In order to cache the result, the full result needs to be retrieved.
            // There is also no difference to that in Hibernate, as Hibernate will first retrieve all elements from the ResultSet.
            List<RE> resultList = emQuery.getResultList();
            cache.put(queryCacheKey, resultList);

            return closing(resultList.stream()).map(this::mapToEntityDelegateUnique);
        } catch (PersistenceException pe) {
            // handle exception that could occur on auto-flush when the query is executed
            throw PersistenceExceptionConverter.convert(pe.getCause() != null ? pe.getCause() : pe);
        }
    }

    private Map<QueryCacheKey, List<RE>> getQueryCache() {
        //noinspection unchecked
        Map<QueryCacheKey, List<RE>> cache = (Map<QueryCacheKey, List<RE>>) em.unwrap(Session.class).getProperties().get(JPA_MAP_CACHE);
        if (cache == null) {
            cache = new HashMap<>();
            em.unwrap(Session.class).setProperty(JPA_MAP_CACHE, cache);
        }
        return cache;
    }

    public static void clearQueryCache(Session session) {
        logger.tracef("query cache cleared");
        //noinspection unchecked
        Map<?, ?> queryCache = (HashMap<Class<?>, Map<?, ?>>) session.getProperties().get(JPA_MAP_CACHE);
        if (queryCache != null) {
            // Can't set null as a property values as it is not serializable. Clearing each map so that the current query result might be saved.
            queryCache.clear();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public long getCount(QueryParameters<M> queryParameters) {
        JpaModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createJpaModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<RE> root = countQuery.from(entityType);
        countQuery.select(cb.count(root));

        JpaPredicateFunction<RE> predicateFunc = mcb.getPredicateFunc();
        if (predicateFunc != null) countQuery.where(predicateFunc.apply(cb, countQuery::subquery, root));

        try {
            return em.createQuery(countQuery).getSingleResult();
        } catch (PersistenceException pe) {
            // handle exception that could occur on auto-flush when the query is executed
            throw PersistenceExceptionConverter.convert(pe.getCause() != null ? pe.getCause() : pe);
        }
    }

    @Override
    public boolean delete(String key) {
        if (key == null) return false;
        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(key);
        if (uuid == null) return false;
        removeFromCache(key);
        em.remove(em.getReference(entityType, uuid));
        logger.tracef("tx %d: delete entity %s", hashCode(), key);
        return true;
    }

    protected void removeFromCache(String key) {
        cacheWithinSession.remove(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public long delete(QueryParameters<M> queryParameters) {
        JpaModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createJpaModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Remove all entities that are in the persistence context and that match the criteria.
        // This avoids calling flush and clear which would detach all other unrelated entities as well.
        int[] removed = {0};
        MapModelCriteriaBuilder<String, E, M> mapMcb = queryParameters.getModelCriteriaBuilder().flashToModelCriteriaBuilder(createCriteriaBuilderMap());
        cacheWithinSession.entrySet().removeIf(entry -> {
            if (mapMcb.getKeyFilter().test(entry.getKey()) && mapMcb.getEntityFilter().test(entry.getValue())) {
                em.remove(em.getReference(entityType, UUIDKey.INSTANCE.fromString(entry.getKey())));
                removed[0]++;
                return true;
            } else {
                return false;
            }
        });

        CriteriaDelete<RE> deleteQuery = cb.createCriteriaDelete(entityType);

        Root<RE> root = deleteQuery.from(entityType);

        JpaPredicateFunction<RE> predicateFunc = mcb.getPredicateFunc();
        if (predicateFunc != null) deleteQuery.where(predicateFunc.apply(cb, deleteQuery::subquery, root));

        try {
            return em.createQuery(deleteQuery).executeUpdate() + removed[0];
        } catch (PersistenceException pe) {
            // handle exception that could occur on auto-flush when the query is executed
            throw PersistenceExceptionConverter.convert(pe.getCause() != null ? pe.getCause() : pe);
        }
    }

    private MapModelCriteriaBuilder<String, E, M> createCriteriaBuilderMap() {
        return new MapModelCriteriaBuilder<>(StringKeyConverter.StringKey.INSTANCE, MapFieldPredicates.getPredicates(modelType));
    }

    @Override
    public void begin() {
        // no-op: rely on JPA transaction enlisted by the JPA storage provider.
    }

    @Override
    public void commit() {
        // no-op: rely on JPA transaction enlisted by the JPA storage provider.
    }

    @Override
    public void rollback() {
        // no-op: rely on JPA transaction enlisted by the JPA storage provider.
    }

    @Override
    public void setRollbackOnly() {
        em.getTransaction().setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        return  em.getTransaction().getRollbackOnly();
    }

    @Override
    public boolean isActive() {
        return em.getTransaction().isActive();
    }

    private Predicate notExpired(final CriteriaBuilder cb, final JpaSubqueryProvider query, final Root<RE> root) {
        return cb.or(cb.greaterThan(root.get("expiration"), Time.currentTimeMillis()),
                    cb.isNull(root.get("expiration")));
    }

    private static class QueryCacheKey {
        private final String queryString;
        private final Integer queryLimit;
        private final Integer queryOffset;
        private final Class<?> modelType;
        private final List<? extends QueryParameters.OrderBy<?>> queryOrderBy;

        public QueryCacheKey(QueryParameters<?> query, Class<?> modelType) {
            // copy over all fields from the query that relevant for caching
            this.queryString = query.getModelCriteriaBuilder().toString();
            this.queryLimit = query.getLimit();
            this.queryOffset = query.getOffset();
            this.queryOrderBy = new ArrayList<>(query.getOrderBy());
            this.modelType = modelType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueryCacheKey that = (QueryCacheKey) o;
            return Objects.equals(queryString, that.queryString)
                    && Objects.equals(queryLimit, that.queryLimit)
                    && Objects.equals(queryOffset, that.queryOffset)
                    && Objects.equals(queryOrderBy, that.queryOrderBy)
                    && Objects.equals(modelType, that.modelType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(queryString, queryLimit, queryOffset, queryOrderBy, modelType);
        }

        @Override
        public String toString() {
            return "QueryCacheKey{" +
                    "queryString='" + queryString + '\'' +
                    ", queryMaxResults=" + queryLimit +
                    ", queryFirstResult=" + queryOffset +
                    ", queryOrderBy=" + queryOrderBy +
                    ", modelType=" + modelType.getName() +
                    '}';
        }
    }
}
