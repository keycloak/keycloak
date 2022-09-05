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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.map.role.MapRoleEntity;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ROLE;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.role.delegate.JpaMapRoleEntityDelegate;
import org.keycloak.models.map.storage.jpa.role.entity.JpaRoleEntity;

public class JpaRoleMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaRoleEntity, MapRoleEntity, RoleModel> {

    @SuppressWarnings("unchecked")
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
}
