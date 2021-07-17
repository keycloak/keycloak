/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.client.delegate;

import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapClientEntityFields;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.client.entity.JpaClientEntity;

public class JpaClientDelegateProvider<T extends MapClientEntity> implements DelegateProvider {

    private JpaClientEntity delegate;
    private final EntityManager em;

    public JpaClientDelegateProvider(JpaClientEntity deledate, EntityManager em) {
        this.delegate = deledate;
        this.em = em;
    }

    @Override
    public JpaClientEntity getDelegate(boolean isRead, Object field, Object... parameters) {
        if (delegate.isMetadataInitialized()) return delegate;
        if (isRead) {
            if (field instanceof MapClientEntityFields) {
                switch ((MapClientEntityFields) field) {
                    case ID:
                    case REALM_ID:
                    case CLIENT_ID:
                    case PROTOCOL:
                    case ENABLED:
                        return delegate;

                    case ATTRIBUTES:
                        CriteriaBuilder cb = em.getCriteriaBuilder();
                        CriteriaQuery<JpaClientEntity> query = cb.createQuery(JpaClientEntity.class);
                        Root<JpaClientEntity> root = query.from(JpaClientEntity.class);
                        root.fetch("attributes", JoinType.INNER);
                        query.select(root).where(cb.equal(root.get("id"), UUID.fromString(delegate.getId())));

                        delegate = em.createQuery(query).getSingleResult();
                        break;

                    default:
                        delegate = em.find(JpaClientEntity.class, UUID.fromString(delegate.getId()));
                }
            } else throw new IllegalStateException("Not a valid client field: " + field);
        } else {
            delegate = em.find(JpaClientEntity.class, UUID.fromString(delegate.getId()));
        }
        return delegate;
    }

}
