/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.jpa.lock.delegate;

import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.lock.MapLockEntity;
import org.keycloak.models.map.lock.MapLockEntityFields;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.lock.entity.JpaLockEntity;

import javax.persistence.EntityManager;
import java.util.UUID;

/**
 * A {@link DelegateProvider} implementation for {@link JpaLockEntity}.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaLockDelegateProvider extends JpaDelegateProvider<JpaLockEntity> implements DelegateProvider<MapLockEntity> {

    private final EntityManager em;

    public JpaLockDelegateProvider(final JpaLockEntity delegate, final EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapLockEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapLockEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapLockEntityFields) {
                switch ((MapLockEntityFields) field) {
                    case ID:
                    case NAME:
                        return getDelegate();

                    default:
                        setDelegate(em.find(JpaLockEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid lock field: " + field);
            }
        } else {
            setDelegate(em.find(JpaLockEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

}
