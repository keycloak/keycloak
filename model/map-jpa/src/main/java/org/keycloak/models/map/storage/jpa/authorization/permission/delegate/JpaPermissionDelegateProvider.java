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
package org.keycloak.models.map.storage.jpa.authorization.permission.delegate;

import java.util.UUID;

import javax.persistence.EntityManager;

import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntityFields;
import org.keycloak.models.map.common.EntityField;
import org.keycloak.models.map.common.delegate.DelegateProvider;
import org.keycloak.models.map.storage.jpa.JpaDelegateProvider;
import org.keycloak.models.map.storage.jpa.authorization.permission.entity.JpaPermissionEntity;

public class JpaPermissionDelegateProvider extends JpaDelegateProvider<JpaPermissionEntity> implements DelegateProvider<MapPermissionTicketEntity> {

    private final EntityManager em;

    public JpaPermissionDelegateProvider(JpaPermissionEntity delegate, EntityManager em) {
        super(delegate);
        this.em = em;
    }

    @Override
    public MapPermissionTicketEntity getDelegate(boolean isRead, Enum<? extends EntityField<MapPermissionTicketEntity>> field, Object... parameters) {
        if (getDelegate().isMetadataInitialized()) return getDelegate();
        if (isRead) {
            if (field instanceof MapPermissionTicketEntityFields) {
                switch ((MapPermissionTicketEntityFields) field) {
                    case ID:
                    case OWNER:
                    case SCOPE_ID:
                    case REALM_ID:
                    case POLICY_ID:
                    case REQUESTER:
                    case RESOURCE_ID:
                    case RESOURCE_SERVER_ID:
                        return getDelegate();

                    default:
                        setDelegate(em.find(JpaPermissionEntity.class, UUID.fromString(getDelegate().getId())));
                }
            } else {
                throw new IllegalStateException("Not a valid resource field: " + field);
            }
        } else {
            setDelegate(em.find(JpaPermissionEntity.class, UUID.fromString(getDelegate().getId())));
        }
        return getDelegate();
    }

}
