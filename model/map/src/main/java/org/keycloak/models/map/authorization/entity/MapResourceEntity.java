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

package org.keycloak.models.map.authorization.entity;

import org.keycloak.models.map.common.AbstractEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MapResourceEntity<K> implements AbstractEntity<K> {
    
    private final K id;
    private String name;
    private String displayName;
    private final Set<String> uris = new HashSet<>();
    private String type;
    private String iconUri;
    private String owner;
    private boolean ownerManagedAccess;
    private String resourceServerId;
    private final Set<String> scopeIds = new HashSet<>();
    private final Set<String> policyIds = new HashSet<>();
    private final Map<String, List<String>> attributes = new HashMap<>();
    private boolean updated = false;

    public MapResourceEntity(K id) {
        this.id = id;
    }

    public MapResourceEntity() {
        this.id = null;
    }

    @Override
    public K getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= !Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.updated |= !Objects.equals(this.displayName, displayName);
        this.displayName = displayName;
    }

    public Set<String> getUris() {
        return uris;
    }

    public void setUris(Set<String> uris) {
        if (Objects.equals(this.uris, uris)) return;

        this.updated = true;
        this.uris.clear();

        if (uris != null) {
            this.uris.addAll(uris);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.updated |= !Objects.equals(this.type, type);
        this.type = type;
    }

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.updated |= !Objects.equals(this.iconUri, iconUri);
        this.iconUri = iconUri;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.updated |= !Objects.equals(this.owner, owner);
        this.owner = owner;
    }

    public boolean isOwnerManagedAccess() {
        return ownerManagedAccess;
    }

    public void setOwnerManagedAccess(boolean ownerManagedAccess) {
        this.updated |= this.ownerManagedAccess != ownerManagedAccess;
        this.ownerManagedAccess = ownerManagedAccess;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.updated |= !Objects.equals(this.resourceServerId, resourceServerId);
        this.resourceServerId = resourceServerId;
    }

    public Set<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(Set<String> scopeIds) {
        if (Objects.equals(this.scopeIds, scopeIds)) return;

        this.updated = true;
        this.scopeIds.clear();
        if (scopeIds != null) {
            this.scopeIds.addAll(scopeIds);
        }
    }

    public Set<String> getPolicyIds() {
        return policyIds;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public List<String> getAttribute(String name) {
        return attributes.get(name);
    }

    public String getSingleAttribute(String name) {
        List<String> attributeValues = attributes.get(name);
        return  attributeValues == null || attributeValues.isEmpty() ? null : attributeValues.get(0);
    }

    public void setAttribute(String name, List<String> value) {
        this.updated |= !Objects.equals(this.attributes.put(name, value), value);
    }

    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
