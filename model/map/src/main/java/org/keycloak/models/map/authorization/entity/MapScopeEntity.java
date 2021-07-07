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

import java.util.Objects;

public class MapScopeEntity<K> implements AbstractEntity<K> {

    private final K id;
    private String name;
    private String displayName;
    private String iconUri;
    private String resourceServerId;
    private boolean updated = false;

    public MapScopeEntity(K id) {
        this.id = id;
    }

    public MapScopeEntity() {
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

    public String getIconUri() {
        return iconUri;
    }

    public void setIconUri(String iconUri) {
        this.updated |= !Objects.equals(this.iconUri, iconUri);
        this.iconUri = iconUri;
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.updated |= !Objects.equals(this.resourceServerId, resourceServerId);
        this.resourceServerId = resourceServerId;
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
