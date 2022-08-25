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
package org.keycloak.models.map.storage.jpa.group;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.group.MapGroupEntity;
import org.keycloak.models.map.group.MapGroupEntityDelegate;
import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_GROUP;
import org.keycloak.models.map.storage.jpa.group.delegate.JpaGroupDelegateProvider;
import org.keycloak.models.map.storage.jpa.group.entity.JpaGroupEntity;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;

public class JpaGroupMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaGroupEntity, MapGroupEntity, GroupModel> {

    @SuppressWarnings("unchecked")
    public JpaGroupMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaGroupEntity.class, GroupModel.class, em);
    }

    @Override
    public Selection<JpaGroupEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaGroupEntity> root) {
        return cb.construct(JpaGroupEntity.class, 
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("name"),
            root.get("parentId")
        );
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_GROUP);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaGroupModelCriteriaBuilder();
    }

    @Override
    protected MapGroupEntity mapToEntityDelegate(JpaGroupEntity original) {
        return new MapGroupEntityDelegate(new JpaGroupDelegateProvider(original, em));
    }
}
