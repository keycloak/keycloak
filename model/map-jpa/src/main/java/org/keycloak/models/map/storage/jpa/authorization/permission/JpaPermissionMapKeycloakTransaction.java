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
package org.keycloak.models.map.storage.jpa.authorization.permission;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authorization.permission.delegate.JpaPermissionDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionEntity;

public class JpaPermissionMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaPermissionEntity, MapPermissionTicketEntity, PermissionTicket> {

    @SuppressWarnings("unchecked")
    public JpaPermissionMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaPermissionEntity.class, PermissionTicket.class, em);
    }

    @Override
    protected Selection<JpaPermissionEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaPermissionEntity> root) {
        return cb.construct(JpaPermissionEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("resourceServerId"),
            root.get("owner"),
            root.get("scopeId"),
            root.get("policyId"),
            root.get("requester"),
            root.get("resourceId"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTHZ_PERMISSION);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaPermissionModelCriteriaBuilder();
    }

    @Override
    protected MapPermissionTicketEntity mapToEntityDelegate(JpaPermissionEntity original) {
        return new MapPermissionTicketEntityDelegate(new JpaPermissionDelegateProvider(original, em));
    }
}
