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
package org.keycloak.models.map.storage.jpa.user;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.user.delegate.JpaUserDelegateProvider;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserEntityDelegate;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_USER;

/**
 * A {@link org.keycloak.models.map.storage.MapKeycloakTransaction} implementation for user entities.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUserMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaUserEntity, MapUserEntity, UserModel> {

    public JpaUserMapKeycloakTransaction(KeycloakSession session,final EntityManager em) {
        super(session, JpaUserEntity.class, UserModel.class, em);
    }

    @Override
    protected Selection<? extends JpaUserEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaUserEntity> root) {
        return cb.construct(JpaUserEntity.class,
                root.get("id"),
                root.get("version"),
                root.get("entityVersion"),
                root.get("realmId"),
                root.get("username"),
                root.get("firstName"),
                root.get("lastName"),
                root.get("email"),
                root.get("emailConstraint"),
                root.get("federationLink"),
                root.get("enabled"),
                root.get("emailVerified"),
                root.get("timestamp")
        );
    }

    @Override
    protected void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_USER);
    }

    @Override
    protected JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaUserModelCriteriaBuilder();
    }

    @Override
    protected MapUserEntity mapToEntityDelegate(JpaUserEntity original) {
        return new MapUserEntityDelegate(new JpaUserDelegateProvider(original, this.em));
    }
}
