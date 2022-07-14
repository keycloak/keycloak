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
package org.keycloak.models.map.storage.jpa.authorization.scope;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.authorization.model.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapScopeEntity;
import org.keycloak.models.map.authorization.entity.MapScopeEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authorization.scope.delagate.JpaScopeDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.scope.entity.JpaScopeEntity;

public class JpaScopeMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaScopeEntity, MapScopeEntity, Scope> {

    @SuppressWarnings("unchecked")
    public JpaScopeMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaScopeEntity.class, Scope.class, em);
    }

    @Override
    protected Selection<JpaScopeEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaScopeEntity> root) {
        return cb.construct(JpaScopeEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("resourceServerId"),
            root.get("name"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTHZ_SCOPE);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaScopeModelCriteriaBuilder();
    }

    @Override
    protected MapScopeEntity mapToEntityDelegate(JpaScopeEntity original) {
        return new MapScopeEntityDelegate(new JpaScopeDelegateProvider(original, em));
    }
}
