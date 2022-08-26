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
package org.keycloak.models.map.storage.jpa.event.admin;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.events.MapAdminEventEntity;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.event.admin.entity.JpaAdminEventEntity;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_ADMIN_EVENT;

/**
 * A {@link org.keycloak.models.map.storage.MapKeycloakTransaction} implementation for admin event entities.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaAdminEventMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaAdminEventEntity, MapAdminEventEntity, AdminEvent>  {

    public JpaAdminEventMapKeycloakTransaction(KeycloakSession session, final EntityManager em) {
        super(session, JpaAdminEventEntity.class, AdminEvent.class, em);
    }

    @Override
    protected Selection<? extends JpaAdminEventEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaAdminEventEntity> root) {
        return root;
    }

    @Override
    protected void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_ADMIN_EVENT);
    }

    @Override
    protected JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaAdminEventModelCriteriaBuilder();
    }

    @Override
    protected MapAdminEventEntity mapToEntityDelegate(JpaAdminEventEntity original) {
        return original;
    }
}
