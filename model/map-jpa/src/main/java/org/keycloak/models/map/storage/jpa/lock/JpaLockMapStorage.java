/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
 * See the License for the specific language governing Locks and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.jpa.lock;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.lock.MapLockEntity;
import org.keycloak.models.map.lock.MapLockEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapStorage;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.lock.delegate.JpaLockDelegateProvider;
import org.keycloak.models.map.storage.jpa.lock.entity.JpaLockEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

public class JpaLockMapStorage extends JpaMapStorage<JpaLockEntity, MapLockEntity, MapLockEntity> {

    @SuppressWarnings("unchecked")
    public JpaLockMapStorage(KeycloakSession session, EntityManager em) {
        super(session, JpaLockEntity.class, MapLockEntity.class, em);
    }

    @Override
    protected Selection<JpaLockEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaLockEntity> root) {
        return cb.construct(JpaLockEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("name"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_LOCK);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaLockModelCriteriaBuilder();
    }

    @Override
    protected MapLockEntity mapToEntityDelegate(JpaLockEntity original) {
        return new MapLockEntityDelegate(new JpaLockDelegateProvider(original, em));
    }
}
