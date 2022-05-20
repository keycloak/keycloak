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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.PersistenceExceptionConverter;
import static org.keycloak.models.map.storage.jpa.PaginationUtils.paginateQuery;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.common.StringKeyConverter.UUIDKey;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;
import static org.keycloak.utils.StreamsUtil.closing;

public abstract class JpaMapKeycloakTransaction<RE extends JpaRootEntity, E extends AbstractEntity, M> implements MapKeycloakTransaction<E, M> {

    private static final Logger logger = Logger.getLogger(JpaMapKeycloakTransaction.class);
    private final Class<RE> entityType;
    protected EntityManager em;

    @SuppressWarnings("unchecked")
    public JpaMapKeycloakTransaction(Class<RE> entityType, EntityManager em) {
        this.em = em;
        this.entityType = entityType;
    }

    protected abstract Selection<? extends RE> selectCbConstruct(CriteriaBuilder cb, Root<RE> root);
    protected abstract void setEntityVersion(JpaRootEntity entity);
    protected abstract JpaModelCriteriaBuilder createJpaModelCriteriaBuilder();
    protected abstract E mapToEntityDelegate(RE original);

    @Override
    @SuppressWarnings("unchecked")
    public E create(E mapEntity) {
        JpaRootEntity jpaEntity = entityType.cast(CLONER.from(mapEntity));
        if (mapEntity.getId() == null) {
            jpaEntity.setId(StringKeyConverter.UUIDKey.INSTANCE.yieldNewUniqueKey().toString());
        }
        logger.tracef("tx %d: create entity %s", hashCode(), jpaEntity.getId());
        setEntityVersion(jpaEntity);
        em.persist(jpaEntity);
        return (E) jpaEntity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E read(String key) {
        if (key == null) return null;
        UUID uuid = StringKeyConverter.UUIDKey.INSTANCE.fromStringSafe(key);
        if (uuid == null) return null;
        return (E) em.find(entityType, uuid);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<E> read(QueryParameters<M> queryParameters) {
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

        BiFunction<CriteriaBuilder, Root<RE>, Predicate> predicateFunc = mcb.getPredicateFunc();
        if (predicateFunc != null) query.where(predicateFunc.apply(cb, root));

        return closing(paginateQuery(em.createQuery(query), queryParameters.getOffset(), queryParameters.getLimit()).getResultStream())
                .map(this::mapToEntityDelegate);
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

        BiFunction<CriteriaBuilder, Root<RE>, Predicate> predicateFunc = mcb.getPredicateFunc();
        if (predicateFunc != null) countQuery.where(predicateFunc.apply(cb, root));

        return em.createQuery(countQuery).getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean delete(String key) {
        if (key == null) return false;
        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(key);
        if (uuid == null) return false;
        em.remove(em.getReference(entityType, uuid));
        logger.tracef("tx %d: delete entity %s", hashCode(), key);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public long delete(QueryParameters<M> queryParameters) {
        JpaModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(createJpaModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaDelete<RE> deleteQuery = cb.createCriteriaDelete(entityType);

        Root<RE> root = deleteQuery.from(entityType);

        BiFunction<CriteriaBuilder, Root<RE>, Predicate> predicateFunc = mcb.getPredicateFunc();
        if (predicateFunc != null) deleteQuery.where(predicateFunc.apply(cb, root));

        return em.createQuery(deleteQuery).executeUpdate();
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
}
