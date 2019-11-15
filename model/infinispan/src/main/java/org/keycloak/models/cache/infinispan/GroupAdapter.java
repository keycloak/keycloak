/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.cache.infinispan;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupAdapter implements GroupModel {

    protected final CachedGroup cached;
    protected final RealmCacheSession cacheSession;
    protected final KeycloakSession keycloakSession;
    protected final RealmModel realm;
    private final Supplier<GroupModel> modelSupplier;
    protected volatile GroupModel updated;

    public GroupAdapter(CachedGroup cached, RealmCacheSession cacheSession, KeycloakSession keycloakSession, RealmModel realm) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.keycloakSession = keycloakSession;
        this.realm = realm;
        modelSupplier = this::getGroupModel;
    }

    protected void getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerGroupInvalidation(cached.getId());
            updated = modelSupplier.get();
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
    }

    protected volatile boolean invalidated;
    public void invalidate() {
        invalidated = true;
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = cacheSession.getRealmDelegate().getGroupById(cached.getId(), realm);
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupModel)) return false;

        GroupModel that = (GroupModel) o;

        if (!cached.getId().equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return cached.getId().hashCode();
    }

    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated()) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        updated.setName(name);

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getDelegateForUpdate();
        updated.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegateForUpdate();
        updated.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegateForUpdate();
        updated.removeAttribute(name);

    }

    @Override
    public String getFirstAttribute(String name) {
        if (isUpdated()) return updated.getFirstAttribute(name);
        return cached.getAttributes(modelSupplier).getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        List<String> values = cached.getAttributes(modelSupplier).get(name);
        if (values == null) return null;
        return values;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return cached.getAttributes(modelSupplier);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        if (isUpdated()) return updated.getRealmRoleMappings();
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> realmMappings = new HashSet<>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                if (((RealmModel) container).getId().equals(realm.getId())) {
                    realmMappings.add(role);
                }
            }
        }
        return realmMappings;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        if (isUpdated()) return updated.getClientRoleMappings(app);
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> appMappings = new HashSet<>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                if (((ClientModel) container).getId().equals(app.getId())) {
                    appMappings.add(role);
                }
            }
        }
        return appMappings;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (isUpdated()) return updated.hasRole(role);
        if (cached.getRoleMappings(modelSupplier).contains(role.getId())) return true;

        Set<RoleModel> mappings = getRoleMappings();
        for (RoleModel mapping: mappings) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (isUpdated()) return updated.getRoleMappings();
        Set<RoleModel> roles = new HashSet<>();
        for (String id : cached.getRoleMappings(modelSupplier)) {
            RoleModel roleById = keycloakSession.realms().getRoleById(id, realm);
            if (roleById == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappings();
            }
            roles.add(roleById);

        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getDelegateForUpdate();
        updated.deleteRoleMapping(role);
    }

    @Override
    public GroupModel getParent() {
        if (isUpdated()) return updated.getParent();
        if (cached.getParentId() == null) return null;
        return keycloakSession.realms().getGroupById(cached.getParentId(), realm);
    }

    @Override
    public String getParentId() {
        if (isUpdated()) return updated.getParentId();
        return cached.getParentId();
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        if (isUpdated()) return updated.getSubGroups();
        Set<GroupModel> subGroups = new HashSet<>();
        for (String id : cached.getSubGroups(modelSupplier)) {
            GroupModel subGroup = keycloakSession.realms().getGroupById(id, realm);
            if (subGroup == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getSubGroups();

            }
            subGroups.add(subGroup);
        }
        return subGroups;
    }



    @Override
    public void setParent(GroupModel group) {
        getDelegateForUpdate();
        updated.setParent(group);

    }

    @Override
    public void addChild(GroupModel subGroup) {
        getDelegateForUpdate();
        updated.addChild(subGroup);

    }

    @Override
    public void removeChild(GroupModel subGroup) {
        getDelegateForUpdate();
        updated.removeChild(subGroup);
    }

    private GroupModel getGroupModel() {
        return cacheSession.getRealmDelegate().getGroupById(cached.getId(), realm);
    }
}
