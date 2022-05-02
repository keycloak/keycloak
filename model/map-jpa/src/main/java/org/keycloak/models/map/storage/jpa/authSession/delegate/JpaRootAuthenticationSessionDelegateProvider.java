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
package org.keycloak.models.map.storage.jpa.authSession.delegate;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntity;
import org.keycloak.models.map.authSession.MapRootAuthenticationSessionEntityFields;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.authSession.entity.JpaRootAuthenticationSessionEntity;

public class JpaRootAuthenticationSessionDelegateProvider extends JpaDelegateProvider<JpaRootAuthenticationSessionEntity> implements DelegateProvider<MapRootAuthenticationSessionEntity> {

    private final EntityManager em;

    public JpaRootAuthenticationSessionDelegateProvider(JpaRootAuthenticationSessionEntity delegate, EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapRootAuthenticationSessionEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapRootAuthenticationSessionEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapRootAuthenticationSessionEntityFields) {
                switch ((MapRootAuthenticationSessionEntityFields) field) {
                    case ID:
                    case REALM_ID:
                    case TIMESTAMP:
                    case EXPIRATION:
                        return getDelegate();

                    default:
                        setDelegate(em.find(JpaRootAuthenticationSessionEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid client field: " + field);
            }
        } else {
            setDelegate(em.find(JpaRootAuthenticationSessionEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

}
