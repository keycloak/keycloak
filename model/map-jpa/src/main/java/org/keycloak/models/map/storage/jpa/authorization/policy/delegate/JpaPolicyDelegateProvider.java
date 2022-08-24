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
package org.keycloak.models.map.storage.jpa.authorization.policy.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.models.map.authorization.entity.MapPolicyEntityFields;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.policy.entity.JpaPolicyEntity;

public class JpaPolicyDelegateProvider extends JpaDelegateProvider<JpaPolicyEntity> implements DelegateProvider<MapPolicyEntity> {

    private final EntityManager em;

    public JpaPolicyDelegateProvider(JpaPolicyEntity delegate, EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapPolicyEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapPolicyEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapPolicyEntityFields) {
                switch ((MapPolicyEntityFields) field) {
                    case ID:
                    case NAME:
                    case TYPE:
                    case OWNER:
                    case REALM_ID:
                    case RESOURCE_SERVER_ID:
                        return getDelegate();

                    default:
                        setDelegate(em.find(JpaPolicyEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid scope field: " + field);
            }
        } else {
            setDelegate(em.find(JpaPolicyEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

}
