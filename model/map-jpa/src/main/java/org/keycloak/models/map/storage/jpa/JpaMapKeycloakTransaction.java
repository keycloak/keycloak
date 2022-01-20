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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.StringKeyConvertor;
import org.keycloak.models.map.common.StringKeyConvertor.UUIDKey;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;
import static org.keycloak.utils.StreamsUtil.closing;

public abstract class JpaMapKeycloakTransaction<RE extends JpaRootEntity, E extends AbstractEntity, M> extends JpaKeycloakTransaction implements MapKeycloakTransaction<E, M> {

    private final Class<RE> entityType;

    @SuppressWarnings("unchecked")
    public JpaMapKeycloakTransaction(Class<RE> entityType, EntityManager em) {
        super(em);
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
        CLONER.from(mapEntity);
        if (mapEntity.getId() == null) {
            jpaEntity.setId(StringKeyConvertor.UUIDKey.INSTANCE.yieldNewUniqueKey().toString());
        }
        setEntityVersion(jpaEntity);
        em.persist(jpaEntity);
        return (E) jpaEntity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E read(String key) {
        if (key == null) return null;
        UUID uuid = StringKeyConvertor.UUIDKey.INSTANCE.fromStringSafe(key);
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

// TODO find out if the flush and clear are needed here or not, since delete(QueryParameters) 
// is not used yet from the code it's difficult to investigate its potential purpose here
// according to https://thorben-janssen.com/5-common-hibernate-mistakes-that-cause-dozens-of-unexpected-queries/#Remove_Child_Entities_With_a_Bulk_Operation
// it seems it is necessary unless it is sure that any of removed entities wasn't fetched
// Once KEYCLOAK-19697 is done we could test our scenarios and see if we need the flush and clear
//        em.flush();
//        em.clear();

        return em.createQuery(deleteQuery).executeUpdate();
    }
}
