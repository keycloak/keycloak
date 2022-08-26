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
package org.keycloak.models.map.storage.jpa.user.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.user.entity.JpaUserEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserEntityFields;

/**
 * A {@link DelegateProvider} implementation for {@link JpaUserEntity}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUserDelegateProvider extends JpaDelegateProvider<JpaUserEntity> implements DelegateProvider<MapUserEntity> {

    private final EntityManager em;

    public JpaUserDelegateProvider(final JpaUserEntity delegate, final EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapUserEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapUserEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapUserEntityFields) {
                switch ((MapUserEntityFields) field) {
                    case ID:
                    case REALM_ID:
                    case USERNAME:
                    case FIRST_NAME:
                    case LAST_NAME:
                    case EMAIL:
                    case EMAIL_CONSTRAINT:
                    case FEDERATION_LINK:
                    case ENABLED:
                    case EMAIL_VERIFIED:
                    case CREATED_TIMESTAMP:
                        return getDelegate();

                    case ATTRIBUTES:
                        this.setDelegateWithAssociation("attributes");
                        break;

                    case USER_CONSENTS:
                        this.setDelegateWithAssociation("consents");
                        break;

                    case FEDERATED_IDENTITIES:
                        this.setDelegateWithAssociation("federatedIdentities");
                        break;

                    default:
                        setDelegate(em.find(JpaUserEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid realm field: " + field);
            }
        } else {
            setDelegate(em.find(JpaUserEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

    protected void setDelegateWithAssociation(final String associationName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JpaUserEntity> query = cb.createQuery(JpaUserEntity.class);
        Root<JpaUserEntity> root = query.from(JpaUserEntity.class);
        root.fetch(associationName, JoinType.LEFT);
        query.select(root).where(cb.equal(root.get("id"), UUID.fromString(getDelegate().getId())));
        setDelegate(em.createQuery(query).getSingleResult());
    }

}
