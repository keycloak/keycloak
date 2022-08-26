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
package org.keycloak.models.map.storage.jpa.authorization.resource;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.authorization.model.Resource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authorization.resource.delegate.JpaResourceDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.resource.entity.JpaResourceEntity;

public class JpaResourceMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaResourceEntity, MapResourceEntity, Resource> {

    @SuppressWarnings("unchecked")
    public JpaResourceMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaResourceEntity.class, Resource.class, em);
    }

    @Override
    protected Selection<JpaResourceEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaResourceEntity> root) {
        return cb.construct(JpaResourceEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("resourceServerId"),
            root.get("name"),
            root.get("type"),
            root.get("owner"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTHZ_RESOURCE);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaResourceModelCriteriaBuilder();
    }

    @Override
    protected MapResourceEntity mapToEntityDelegate(JpaResourceEntity original) {
        return new MapResourceEntityDelegate(new JpaResourceDelegateProvider(original, em));
    }
}
