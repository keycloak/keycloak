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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.keycloak.models.map.common.AbstractEntity;

public class MapRoleEntity<K> implements AbstractEntity<K> {

    private K id;
    private String realmId;

    private String name;
    private String description;
    private boolean clientRole;
    private String clientId;
    private Set<String> compositeRoles = new HashSet<>();
    private Map<String, List<String>> attributes = new HashMap<>();

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    protected MapRoleEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapRoleEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated;
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
        this.updated |= ! Objects.equals(this.attributes.put(name, values), values);
    }

    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.updated |= ! Objects.equals(this.realmId, realmId);
        this.realmId = realmId;
    }

    public boolean isClientRole() {
        return clientRole;
    }

    public void setClientRole(boolean clientRole) {
        this.updated |= ! Objects.equals(this.clientRole, clientRole);
        this.clientRole = clientRole;
    }

    public boolean isComposite() {
        return ! (compositeRoles == null || compositeRoles.isEmpty());
    }

    public Set<String> getCompositeRoles() {
        return compositeRoles;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.updated |= ! Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    public void setCompositeRoles(Set<String> compositeRoles) {
        this.updated |= ! Objects.equals(this.compositeRoles, compositeRoles);
        this.compositeRoles.clear();
        this.compositeRoles.addAll(compositeRoles);
    }

    public void addCompositeRole(String roleId) {
        this.updated |= this.compositeRoles.add(roleId);
    }

    public void removeCompositeRole(String roleId) {
        this.updated |= this.compositeRoles.remove(roleId);
    }
}
