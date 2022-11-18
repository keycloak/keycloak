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
package org.keycloak.models.map.storage.jpa.realm;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityDelegate;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.realm.delegate.JpaRealmDelegateProvider;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaRealmEntity;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_REALM;

/**
 * A {@link org.keycloak.models.map.storage.MapKeycloakTransaction} implementation for realm entities.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaRealmMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaRealmEntity, MapRealmEntity, RealmModel> {

    public JpaRealmMapKeycloakTransaction(KeycloakSession session, final EntityManager em) {
        super(session, JpaRealmEntity.class, RealmModel.class, em);
    }

    @Override
    protected Selection<? extends JpaRealmEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaRealmEntity> root) {
        return cb.construct(JpaRealmEntity.class,
                root.get("id"),
                root.get("version"),
                root.get("entityVersion"),
                root.get("name"),
                root.get("displayName"),
                root.get("displayNameHtml"),
                root.get("enabled")
        );
    }

    @Override
    protected void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_REALM);
    }

    @Override
    protected JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaRealmModelCriteriaBuilder();
    }

    @Override
    protected MapRealmEntity mapToEntityDelegate(JpaRealmEntity original) {
        return new MapRealmEntityDelegate(new JpaRealmDelegateProvider(original, em));
    }
}
