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
package org.keycloak.models.map.storage.jpa.singleUseObject;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;

import org.keycloak.models.ActionTokenValueModel;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity;
import org.keycloak.models.map.storage.jpa.JpaMapKeycloakTransaction;
import org.keycloak.models.map.storage.jpa.JpaModelCriteriaBuilder;
import org.keycloak.models.map.storage.jpa.JpaRootEntity;
import org.keycloak.models.map.storage.jpa.singleUseObject.entity.JpaSingleUseObjectEntity;

import static org.keycloak.models.map.storage.jpa.Constants.CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT;

/**
 * A {@link org.keycloak.models.map.storage.MapKeycloakTransaction} implementation for single-use object entities.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaSingleUseObjectMapKeycloakTransaction extends JpaMapKeycloakTransaction<JpaSingleUseObjectEntity, MapSingleUseObjectEntity, ActionTokenValueModel> {

    public JpaSingleUseObjectMapKeycloakTransaction(final EntityManager em) {
        super(JpaSingleUseObjectEntity.class, ActionTokenValueModel.class, em);
    }

    @Override
    protected Selection<? extends JpaSingleUseObjectEntity> selectCbConstruct(CriteriaBuilder cb, Root<JpaSingleUseObjectEntity> root) {
        return root;
    }

    @Override
    protected void setEntityVersion(JpaRootEntity entity) {
        entity.setEntityVersion(CURRENT_SCHEMA_VERSION_SINGLE_USE_OBJECT);
    }

    @Override
    protected JpaModelCriteriaBuilder createJpaModelCriteriaBuilder() {
        return new JpaSingleUseObjectModelCriteriaBuilder();
    }

    @Override
    protected MapSingleUseObjectEntity mapToEntityDelegate(JpaSingleUseObjectEntity original) {
        return original;
    }
}
