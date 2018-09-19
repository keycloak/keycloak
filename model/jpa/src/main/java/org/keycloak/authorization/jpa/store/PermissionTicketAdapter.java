/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.jpa.store;

import static org.keycloak.authorization.UserManagedPermissionUtil.updatePolicy;

import javax.persistence.EntityManager;

import org.keycloak.authorization.jpa.entities.PermissionTicketEntity;
import org.keycloak.authorization.jpa.entities.PolicyEntity;
import org.keycloak.authorization.jpa.entities.ScopeEntity;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.jpa.JpaModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PermissionTicketAdapter implements PermissionTicket, JpaModel<PermissionTicketEntity> {

    private final EntityManager entityManager;
    private final PermissionTicketEntity entity;
    private final StoreFactory storeFactory;

    public PermissionTicketAdapter(PermissionTicketEntity entity, EntityManager entityManager, StoreFactory storeFactory) {
        this.entity = entity;
        this.entityManager = entityManager;
        this.storeFactory = storeFactory;
    }

    @Override
    public PermissionTicketEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getOwner() {
        return entity.getOwner();
    }

    @Override
    public String getRequester() {
        return entity.getRequester();
    }

    @Override
    public boolean isGranted() {
        return entity.isGranted();
    }

    @Override
    public Long getCreatedTimestamp() {
        return entity.getCreatedTimestamp();
    }

    @Override
    public Long getGrantedTimestamp() {
        return entity.getGrantedTimestamp();
    }

    @Override
    public void setGrantedTimestamp(Long millis) {
        entity.setGrantedTimestamp(millis);
        updatePolicy(this, storeFactory);
    }

    @Override
    public ResourceServer getResourceServer() {
        return storeFactory.getResourceServerStore().findById(entity.getResourceServer().getId());
    }

    @Override
    public Policy getPolicy() {
        PolicyEntity policy = entity.getPolicy();

        if (policy == null) {
            return null;
        }

        return storeFactory.getPolicyStore().findById(policy.getId(), entity.getResourceServer().getId());
    }

    @Override
    public void setPolicy(Policy policy) {
        if (policy != null) {
            entity.setPolicy(entityManager.getReference(PolicyEntity.class, policy.getId()));
        }
    }

    @Override
    public Resource getResource() {
        return storeFactory.getResourceStore().findById(entity.getResource().getId(), getResourceServer().getId());
    }

    @Override
    public Scope getScope() {
        ScopeEntity scope = entity.getScope();

        if (scope == null) {
            return null;
        }

        return storeFactory.getScopeStore().findById(scope.getId(), getResourceServer().getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Policy)) return false;

        PermissionTicket that = (PermissionTicket) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public static PermissionTicketEntity toEntity(EntityManager em, PermissionTicket permission) {
        if (permission instanceof PermissionTicketAdapter) {
            return ((PermissionTicketAdapter)permission).getEntity();
        } else {
            return em.getReference(PermissionTicketEntity.class, permission.getId());
        }
    }



}
