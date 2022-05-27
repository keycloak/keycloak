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
package org.keycloak.models.map.storage.jpa.authorization.policy;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import org.keycloak.authorization.model.Policy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntityDelegate;
import org.keycloak.models.map.storage.jpa.Constants;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.authorization.policy.delegate.JpaPolicyDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyEntity;

public class JpaPolicyMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaPolicyEntity, MapPolicyEntity, Policy> {

    @SuppressWarnings("unchecked")
    public JpaPolicyMapKeycloakTransaction(KeycloakSession session, EntityManager em) {
        super(session, JpaPolicyEntity.class, Policy.class, em);
    }

    @Override
    protected Selection<JpaPolicyEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaPolicyEntity> root) {
        return cb.construct(JpaPolicyEntity.class,
            root.get("id"),
            root.get("version"),
            root.get("entityVersion"),
            root.get("realmId"),
            root.get("resourceServerId"),
            root.get("name"),
            root.get("owner"),
            root.get("type"));
    }

    @Override
    public void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(Constants.CURRENT_SCHEMA_VERSION_AUTHZ_POLICY);
    }

    @Override
    public JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaPolicyModelCriteriaBuilder();
    }

    @Override
    protected MapPolicyEntity mapToEntityDelegate(JpaPolicyEntity original) {
        return new MapPolicyEntityDelegate(new JpaPolicyDelegateProvider(original, em));
    }
}
