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
package org.keycloak.models.map.storage.jpa.authSession;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntityDelegate;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_AUTH_SESSION;

import org.keycloak.models.map.storage.jpa.JpaMapStorage;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authSession.delegate.JpaRootAuthenticationSessionDelegateProvider;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;
import org.keycloak.sessions.RootAuthenticationSessionModel;

public class JpaRootAuthenticationSessionMapStorage extends JpaMapStorage<JpaRootAuthenticationSessionEntity, MapRootAuthenticationSessionEntity, RootAuthenticationSessionModel> {

    public JpaRootAuthenticationSessionMapStorage(KeycloakSession session, EntityManager em) {
        super(session, JpaRootAuthenticationSessionEntity.class, RootAuthenticationSessionModel.class, em);
    }

    @Override
    public Selection<JpaRootAuthenticationSessionEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaRootAuthenticationSessionEntity> root) {
        return cb.construct(JpaRootAuthenticationSessionEntity.class, 
            root.get("id"), 
            root.get("version"),
            root.get("entityVersion"), 
            root.get("realmId"), 
            root.get("timestamp"),
            root.get("expiration")
        );
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_AUTH_SESSION);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaRootAuthenticationSessionModelCriteriaBuilder();
    }

    @Override
    protected MapRootAuthenticationSessionEntity mapToEntityDelegate(JpaRootAuthenticationSessionEntity original) {
        return new MapRootAuthenticationSessionEntityDelegate(new JpaRootAuthenticationSessionDelegateProvider(original, em));
    }

}
