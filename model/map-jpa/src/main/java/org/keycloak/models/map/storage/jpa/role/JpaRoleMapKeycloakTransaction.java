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

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.hibernate.query.NativeQuery;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.common.StringKeyConverter;
import org.keycloak.models.map.role.MapRoleEntity;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ROLE;

import org.keycloak.models.map.storage.MapKeycloakTransactionWithHasRole;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.role.delegate.JpaMapRoleEntityDelegate;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleCompositeEntity;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JpaRoleMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaRoleEntity, MapRoleEntity, RoleModel>
        implements MapKeycloakTransactionWithHasRole<MapRoleEntity, RoleModel> {

    public JpaRoleMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaRoleEntity.class, RoleModel.class, em);
    }

    @Override
    public Selection<JpaRoleEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaRoleEntity> root) {
        return cb.construct(JpaRoleEntity.class, 
            root.get("id"), 
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("clientId"),
            root.get("name"),
            root.get("description")
        );
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_ROLE);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaRoleModelCriteriaBuilder();
    }

    @Override
    protected MapRoleEntity mapToEntityDelegate(JpaRoleEntity original) {
        return new JpaMapRoleEntityDelegate(original, em);
    }

    @Override
    public boolean hasRole(String realmId, Set<String> roleIds, Set<String> targetRoleIds) {
        // TODO: add prevention to infinite recursion
        Query nativeQuery = em.createNativeQuery("WITH RECURSIVE rr AS (SELECT cr.role_id as role_id, cr.child_role_id as child_role_id " +
                "           FROM kc_role_composite cr JOIN kc_role kr on kr.id = cr.role_id WHERE cr.role_id IN :roleIds AND kr.realmid = :realmId " +
                "UNION ALL " +
                "SELECT cr.role_id, cr.child_role_id FROM rr JOIN kc_role kr ON kr.id = uuid(rr.child_role_id) JOIN kc_role_composite cr ON kr.id = cr.role_id) " +
                "select rr.child_role_id from rr where rr.child_role_id IN :targetRoleIds");
        //noinspection unchecked
        nativeQuery.unwrap(NativeQuery.class)
                .addSynchronizedEntityClass(JpaRoleEntity.class, JpaRoleCompositeEntity.class);
        nativeQuery.setParameter("roleIds", roleIds.stream().map(StringKeyConverter.UUIDKey.INSTANCE::fromString).collect(Collectors.toSet()));
        nativeQuery.setParameter("targetRoleIds", targetRoleIds);
        nativeQuery.setParameter("realmId", realmId);
        nativeQuery.setMaxResults(1);
        List<?> result = nativeQuery.getResultList();
        return result.size() > 0;
    }

    @Override
    public Set<MapRoleEntity> expandCompositeRoles(String realmId, Set<String> targetRoles) {
        // TODO: add prevention to infinite recursion
        Query nativeQuery = em.createNativeQuery("WITH RECURSIVE rr AS (SELECT cr.role_id as role_id, cr.child_role_id as child_role_id " +
                "           FROM kc_role_composite cr JOIN kc_role kr on kr.id = cr.role_id WHERE cr.role_id IN :targetRoles AND kr.realmid = :realmId " +
                "UNION ALL " +
                "SELECT cr.role_id, cr.child_role_id FROM rr JOIN kc_role kr ON kr.id = uuid(rr.child_role_id) JOIN kc_role_composite cr ON kr.id = cr.role_id) " +
                "SELECT DISTINCT rr.child_role_id FROM rr");
        //noinspection unchecked
        nativeQuery.unwrap(NativeQuery.class)
                .addSynchronizedEntityClass(JpaRoleEntity.class, JpaRoleCompositeEntity.class);
        nativeQuery.setParameter("targetRoles", targetRoles.stream().map(StringKeyConverter.UUIDKey.INSTANCE::fromString).collect(Collectors.toSet()));
        nativeQuery.setParameter("realmId", realmId);
        Set<MapRoleEntity> result = new HashSet<>();
        @SuppressWarnings("unchecked") List<String> sqlResult = (List<String>) nativeQuery.getResultList();
        for (String targetRole : targetRoles) {
            result.add(read(targetRole));
        }
        for (String targetRole : sqlResult) {
            result.add(read(targetRole));
        }
        return result;
    }
}
