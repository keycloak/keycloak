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
package org.keycloak.models.map.storage.jpa.realm.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.MapRealmEntityFields;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.realm.entity.JpaRealmEntity;

/**
 * A {@link DelegateProvider} implementation for {@link JpaRealmEntity}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaRealmDelegateProvider extends JpaDelegateProvider<JpaRealmEntity> implements DelegateProvider<MapRealmEntity> {

    private final EntityManager em;

    public JpaRealmDelegateProvider(final JpaRealmEntity delegate, final EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapRealmEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapRealmEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapRealmEntityFields) {
                switch ((MapRealmEntityFields) field) {
                    case ID:
                    case NAME:
                    case DISPLAY_NAME:
                    case DISPLAY_NAME_HTML:
                    case ENABLED:
                        return getDelegate();

                    case ATTRIBUTES:
                        this.setDelegateWithAssociation("attributes");
                        break;

                    case COMPONENTS:
                        this.setDelegateWithAssociation("components");
                        break;

                    default:
                        setDelegate(em.find(JpaRealmEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid realm field: " + field);
            }
        } else {
            setDelegate(em.find(JpaRealmEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

    protected void setDelegateWithAssociation(final String associationName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JpaRealmEntity> query = cb.createQuery(JpaRealmEntity.class);
        Root<JpaRealmEntity> root = query.from(JpaRealmEntity.class);
        root.fetch(associationName, JoinType.LEFT);
        query.select(root).where(cb.equal(root.get("id"), UUID.fromString(getDelegate().getId())));
        setDelegate(em.createQuery(query).getSingleResult());
    }
}
