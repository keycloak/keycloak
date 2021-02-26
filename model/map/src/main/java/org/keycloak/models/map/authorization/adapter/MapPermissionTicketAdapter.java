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

package org.keycloak.models.map.authorization.adapter;


import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;


import org.keycloak.models.map.authorization.entity.MapPermissionTicketEntity;
import static org.keycloak.authorization.UserManagedPermissionUtil.updatePolicy;

public abstract class MapPermissionTicketAdapter<K extends Comparable<K>> extends AbstractPermissionTicketModel<MapPermissionTicketEntity<K>> {
    
    public MapPermissionTicketAdapter(MapPermissionTicketEntity<K> entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
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
    public Resource getResource() {
        return storeFactory.getResourceStore().findById(entity.getResourceId(), entity.getResourceServerId());
    }

    @Override
    public Scope getScope() {
        if (entity.getScopeId() == null) return null;
        return storeFactory.getScopeStore().findById(entity.getScopeId(), entity.getResourceServerId());
    }

    @Override
    public boolean isGranted() {
        return entity.getGrantedTimestamp() != null;
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
        return storeFactory.getResourceServerStore().findById(entity.getResourceServerId());
    }

    @Override
    public Policy getPolicy() {
        if (entity.getPolicyId() == null) return null;
        return storeFactory.getPolicyStore().findById(entity.getPolicyId(), entity.getResourceServerId());
    }

    @Override
    public void setPolicy(Policy policy) {
        if (policy != null) {
            entity.setPolicyId(policy.getId());
        }
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
