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
package org.keycloak.models.map.storage.jpa.authorization.resourceServer;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntity;
import org.keycloak.models.map.authorization.entity.MapResourceServerEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.delegate.JpaResourceServerDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.resourceServer.entity.JpaResourceServerEntity;

public class JpaResourceServerMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaResourceServerEntity, MapResourceServerEntity, ResourceServer> {

    @SuppressWarnings("unchecked")
    public JpaResourceServerMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaResourceServerEntity.class, ResourceServer.class, em);
    }

    @Override
    protected Selection<JpaResourceServerEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaResourceServerEntity> root) {
        return cb.construct(JpaResourceServerEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("clientId"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE_SERVER);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaResourceServerModelCriteriaBuilder();
    }

    @Override
    protected MapResourceServerEntity mapToEntityDelegate(JpaResourceServerEntity original) {
        return new MapResourceServerEntityDelegate(new JpaResourceServerDelegateProvider(original, em));
    }
}
