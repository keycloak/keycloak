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
package org.keycloak.models.map.storage.jpa.role;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import org.keycloak.models.RoleModel;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import org.keycloak.models.map.common.StringKeyConvertor.UUIDKey;
import org.keycloak.models.map.role.MapRoleEntity;
import org.keycloak.models.map.role.MapRoleEntityDelegate;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.QueryParameters;
import static org.keycloak.models.map.storage.jpa.Constants.SUPPORTED_VERSION_ROLE;
import static org.keycloak.models.map.storage.jpa.JpaMapStorageProviderFactory.CLONER;
import org.keycloak.models.map.storage.jpa.role.delegate.JpaRoleDelegateProvider;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;
import static org.keycloak.utils.StreamsUtil.closing;

public class JpaRoleMapKeycloakTransaction extends JpaKeycloakTransaction implements MapKeycloakTransaction<MapRoleEntity, RoleModel> {

    public JpaRoleMapKeycloakTransaction(EntityManager em) {
        super(em);
    }

    @Override
    public MapRoleEntity create(MapRoleEntity mapEntity) {
        JpaRoleEntity jpaEntity = (JpaRoleEntity) CLONER.from(mapEntity);
        if (mapEntity.getId() == null) {
            jpaEntity.setId(UUIDKey.INSTANCE.yieldNewUniqueKey().toString());
        }
        jpaEntity.setEntityVersion(SUPPORTED_VERSION_ROLE);
        em.persist(jpaEntity);
        return jpaEntity;
    }

    @Override
    public MapRoleEntity read(String key) {
        if (key == null) return null;
        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(key);
        if (uuid == null) return null;

        return em.find(JpaRoleEntity.class, uuid);
    }

    @Override
    public Stream<MapRoleEntity> read(QueryParameters<RoleModel> queryParameters) {
        JpaRoleModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(new JpaRoleModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JpaRoleEntity> query = cb.createQuery(JpaRoleEntity.class);
        Root<JpaRoleEntity> root = query.from(JpaRoleEntity.class);
        query.select(cb.construct(JpaRoleEntity.class, 
                root.get("id"), 
                root.get("entityVersion"),
                root.get("realmId"),
                root.get("clientId"),
                root.get("name"),
                root.get("description")
        ));

        if (!queryParameters.getOrderBy().isEmpty()) {
            List<Order> orderByList = new LinkedList<>();
            for (QueryParameters.OrderBy<RoleModel> order : queryParameters.getOrderBy()) {
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

        if (mcb.getPredicateFunc() != null) query.where(mcb.getPredicateFunc().apply(cb, root));

        return closing(
                paginateQuery(em.createQuery(query), queryParameters.getOffset(), queryParameters.getLimit())
                        .getResultStream())
                .map(r -> new MapRoleEntityDelegate(new JpaRoleDelegateProvider(r, em)));
    }

    @Override
    public long getCount(QueryParameters<RoleModel> queryParameters) {
        JpaRoleModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(new JpaRoleModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<JpaRoleEntity> root = countQuery.from(JpaRoleEntity.class);
        countQuery.select(cb.count(root));

        if (mcb.getPredicateFunc() != null) countQuery.where(mcb.getPredicateFunc().apply(cb, root));

        return em.createQuery(countQuery).getSingleResult();
    }

    @Override
    public boolean delete(String key) {
        if (key == null) return false;
        UUID uuid = UUIDKey.INSTANCE.fromStringSafe(key);
        if (uuid == null) return false;
        em.remove(em.getReference(JpaRoleEntity.class, uuid));
        return true;
    }

    @Override
    public long delete(QueryParameters<RoleModel> queryParameters) {
        JpaRoleModelCriteriaBuilder mcb = queryParameters.getModelCriteriaBuilder()
                .flashToModelCriteriaBuilder(new JpaRoleModelCriteriaBuilder());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaDelete<JpaRoleEntity> deleteQuery = cb.createCriteriaDelete(JpaRoleEntity.class);

        Root<JpaRoleEntity> root = deleteQuery.from(JpaRoleEntity.class);

        if (mcb.getPredicateFunc() != null) deleteQuery.where(mcb.getPredicateFunc().apply(cb, root));

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
