/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file.adapter;

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RoleModel for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class RoleAdapter implements RoleModel {

    private final RoleEntity role;
    private RoleContainerModel roleContainer;
    private final RealmModel realm;

    private final Set<RoleModel> compositeRoles = new HashSet<RoleModel>();

    public RoleAdapter(RealmModel realm, RoleEntity roleEntity) {
        this(realm, roleEntity, null);
    }

    public RoleAdapter(RealmModel realm, RoleEntity roleEntity, RoleContainerModel roleContainer) {
        this.role = roleEntity;
        this.roleContainer = roleContainer;
        this.realm = realm;
    }

    public RoleEntity getRoleEntity() {
        return this.role;
    }

    public boolean isRealmRole() {
        return role.getRealmId() != null;
    }

    @Override
    public String getId() {
        return role.getId();
    }

    @Override
    public String getName() {
        return role.getName();
    }

    @Override
    public void setName(String name) {
        RealmAdapter realmAdapter = (RealmAdapter)realm;
        if (role.getName().equals(name)) return;
        if (realmAdapter.hasRoleWithName(name)) throw new ModelDuplicateException("Role name " + name + " already exists.");
        role.setName(name);
    }

    @Override
    public String getDescription() {
        return role.getDescription();
    }

    @Override
    public void setDescription(String description) {
        role.setDescription(description);
    }

    @Override
    public boolean isScopeParamRequired() {
        return role.isScopeParamRequired();
    }

    @Override
    public void setScopeParamRequired(boolean scopeParamRequired) {
        role.setScopeParamRequired(scopeParamRequired);
    }

    @Override
    public boolean isComposite() {
        return role.getCompositeRoleIds() != null && role.getCompositeRoleIds().size() > 0;
    }

    @Override
    public void addCompositeRole(RoleModel childRole) {
        List<String> compositeRoleIds = role.getCompositeRoleIds();
        if (compositeRoleIds == null) compositeRoleIds = new ArrayList<String>();
        compositeRoleIds.add(childRole.getId());
        role.setCompositeRoleIds(compositeRoleIds);
        compositeRoles.add(childRole);
    }

    /**
     * Recursively remove composite roles for the specified app
     * @param appId
     */
    public void removeApplicationComposites(String appId) {
        if (!isComposite()) return;
        Set<RoleModel> toBeRemoved = new HashSet<RoleModel>();
        for (RoleModel compositeRole : getComposites()) {
            RoleAdapter roleAdapter = (RoleAdapter)compositeRole;
            if (appId.equals(roleAdapter.getRoleEntity().getClientId())) {
                toBeRemoved.add(compositeRole);
            } else {
                roleAdapter.removeApplicationComposites(appId);
            }
        }

        for (RoleModel compositeRole : toBeRemoved) {
            removeCompositeRole(compositeRole);
        }
    }

    @Override
    public void removeCompositeRole(RoleModel childRole) {
        compositeRoles.remove(childRole);
        List<String> compositeRoleIds = role.getCompositeRoleIds();
        if (compositeRoleIds == null) return; // shouldn't happen
        compositeRoleIds.remove(childRole.getId());
        role.setCompositeRoleIds(compositeRoleIds);
    }

    @Override
    public Set<RoleModel> getComposites() {
        return Collections.unmodifiableSet(compositeRoles);
    }

    @Override
    public RoleContainerModel getContainer() {
        if (roleContainer == null) {
            // Compute it
            if (role.getRealmId() != null) {
                roleContainer = realm;//new RealmAdapter(session, realm);
            } else if (role.getClientId() != null) {
                roleContainer = realm.getClientById(role.getClientId());//new ApplicationAdapter(session, realm, appEntity);
            } else {
                throw new IllegalStateException("Both realmId and applicationId are null for role: " + this);
            }
        }
        return roleContainer;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (this.equals(role)) return true;
        if (!isComposite()) return false;

        Set<RoleModel> visited = new HashSet<RoleModel>();
        return KeycloakModelUtils.searchFor(role, this, visited);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof RoleModel)) return false;

        RoleModel that = (RoleModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
