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

package org.keycloak.models.map.authorization.adapter;

import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;

import org.keycloak.models.RealmModel;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MapResourceAdapter extends AbstractResourceModel<MapResourceEntity> {

    private final RealmModel realm;
    private ResourceServer resourceServer;

    public MapResourceAdapter(RealmModel realm, ResourceServer resourceServer, MapResourceEntity entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
        Objects.requireNonNull(realm);
        this.realm = realm;
        this.resourceServer = resourceServer;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        throwExceptionIfReadonly();
        entity.setName(name);
    }

    @Override
    public String getDisplayName() {
        return entity.getDisplayName();
    }

    @Override
    public void setDisplayName(String name) {
        throwExceptionIfReadonly();
        entity.setDisplayName(name);
    }

    @Override
    public Set<String> getUris() {
        Set<String> uris = entity.getUris();
        return uris == null ? Collections.emptySet() : entity.getUris();
    }

    @Override
    public void updateUris(Set<String> uri) {
        throwExceptionIfReadonly();
        entity.setUris(uri);
    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public void setType(String type) {
        throwExceptionIfReadonly();
        entity.setType(type);
    }

    @Override
    public List<Scope> getScopes() {
        Set<String> ids = entity.getScopeIds();
        ResourceServer resourceServer = getResourceServer();
        return ids == null ? Collections.emptyList() : ids.stream()
                .map(id -> storeFactory
                        .getScopeStore().findById(realm, resourceServer, id))
                .collect(Collectors.toList());
    }

    @Override
    public String getIconUri() {
        return entity.getIconUri();
    }

    @Override
    public void setIconUri(String iconUri) {
        throwExceptionIfReadonly();
        entity.setIconUri(iconUri);
    }

    @Override
    public ResourceServer getResourceServer() {
        if (resourceServer == null) {
            resourceServer = storeFactory.getResourceServerStore().findById(realm, entity.getResourceServerId());
        }
        return resourceServer;
    }

    @Override
    public String getOwner() {
        return entity.getOwner();
    }

    @Override
    public boolean isOwnerManagedAccess() {
        Boolean isOMA = entity.isOwnerManagedAccess();
        return isOMA == null ? false : isOMA;
    }

    @Override
    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
        throwExceptionIfReadonly();
        entity.setOwnerManagedAccess(ownerManagedAccess);
    }

    @Override
    public void updateScopes(Set<Scope> scopes) {
        throwExceptionIfReadonly();

        PermissionTicketStore permissionStore = storeFactory.getPermissionTicketStore();
        PolicyStore policyStore = storeFactory.getPolicyStore();

        for (Scope scope : getScopes()) {
            if (!scopes.contains(scope)) {
                // The scope^ was removed from the Resource

                // Remove permission tickets based on the scope
                List<PermissionTicket> permissions = permissionStore.findByScope(getResourceServer(), scope);
                for (PermissionTicket permission : permissions) {
                    permissionStore.delete(realm, permission.getId());
                }

                // Remove the scope from each Policy for this Resource
                policyStore.findByResource(getResourceServer(), this, policy -> policy.removeScope(scope));
            }
        }

        entity.setScopeIds(scopes.stream().map(Scope::getId).collect(Collectors.toSet()));
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = entity.getAttributes();
        return attrs == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(attrs));
    }

    @Override
    public String getSingleAttribute(String name) {
        List<String> attributeValues = entity.getAttribute(name);
        return  attributeValues == null || attributeValues.isEmpty() ? null : attributeValues.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        return entity.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throwExceptionIfReadonly();
        entity.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        throwExceptionIfReadonly();
        entity.removeAttribute(name);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
