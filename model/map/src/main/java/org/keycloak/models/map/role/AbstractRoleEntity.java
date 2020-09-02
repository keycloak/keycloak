/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.role;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.keycloak.models.map.common.AbstractEntity;

public abstract class AbstractRoleEntity<K> implements AbstractEntity<K> {

    private K id;
    private String containerId;

    private String name;
    private String description;
    private boolean clientRole;
    private String clientId;
    private Set<K> compositeRoles = new HashSet<>();
    private Map<String, List<String>> attributes = new HashMap<>();

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    protected AbstractRoleEntity() {
        this.id = null;
        this.containerId = null;
    }

    public AbstractRoleEntity(K id, String containerId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(containerId, "containerId");

        this.id = id;
        this.containerId = containerId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.updated |= ! Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated |= ! Objects.equals(this.description, description);
        this.description = description;
    }

    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.updated |= ! Objects.equals(this.attributes, attributes);
        this.attributes = attributes;
    }

    public void setAttribute(String name, List<String> values) {
        this.updated = true;
        this.attributes.put(name, values);
    }

    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.updated |= ! Objects.equals(this.containerId, containerId);
        this.containerId = containerId;
    }

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.updated |= ! Objects.equals(this.clientRole, clientRole);
        this.clientRole = clientRole;
    }

    public Set<K> getCompositeRoles() {
        return compositeRoles;
    }

    public void setCompositeRoles(Set<K> compositeRoles) {
        this.updated |= ! Objects.equals(this.compositeRoles, compositeRoles);
        this.compositeRoles.clear();
        this.compositeRoles.addAll(compositeRoles);
    }

    public void addCompositeRole(K roleId) {
        this.updated |= ! this.compositeRoles.contains(roleId);
        this.compositeRoles.add(roleId);
    }

    public void removeCompositeRole(K roleId) {
        
    }
}
