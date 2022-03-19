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
package org.keycloak.models.map.storage.jpa.clientscope.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.keycloak.models.map.clientscope.MapClientScopeEntity;
import org.keycloak.models.map.clientscope.MapClientScopeEntityFields;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.clientscope.entity.JpaClientScopeEntity;

public class JpaClientScopeDelegateProvider extends JpaDelegateProvider<JpaClientScopeEntity> implements DelegateProvider<MapClientScopeEntity> {

    private final EntityManager em;

    public JpaClientScopeDelegateProvider(JpaClientScopeEntity delegate, EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapClientScopeEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapClientScopeEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapClientScopeEntityFields) {
                switch ((MapClientScopeEntityFields) field) {
                    case ID:
                    case REALM_ID:
                    case NAME:
                        return getDelegate();

                    case ATTRIBUTES:
                        CriteriaBuilder cb = em.getCriteriaBuilder();
                        CriteriaQuery<JpaClientScopeEntity> query = cb.createQuery(JpaClientScopeEntity.class);
                        Root<JpaClientScopeEntity> root = query.from(JpaClientScopeEntity.class);
                        root.fetch("attributes", JoinType.LEFT);
                        query.select(root).where(cb.equal(root.get("id"), UUID.fromString(getDelegate().getId())));

                        setDelegate(em.createQuery(query).getSingleResult());
                        break;

                    default:
                        setDelegate(em.find(JpaClientScopeEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid client scope field: " + field);
            }
        } else {
            setDelegate(em.find(JpaClientScopeEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

}
