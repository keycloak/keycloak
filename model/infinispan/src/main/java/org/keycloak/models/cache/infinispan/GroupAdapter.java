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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.entities.CachedGroup;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

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
        modelSupplier = new LazyModel<>(this::getGroupModel);
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
        updated = cacheSession.getGroupDelegate().getGroupById(realm, cached.getId());
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
    public String getDescription() {
        if (isUpdated()) return updated.getDescription();
        return cached.getDescription();
    }

    @Override
    public void setDescription(String description) {
        getDelegateForUpdate();
        updated.setDescription(description);
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
        return cached.getAttributes(keycloakSession, modelSupplier).getFirst(name);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (isUpdated()) return updated.getAttributeStream(name);
        List<String> values = cached.getAttributes(keycloakSession, modelSupplier).get(name);
        if (values == null) return Stream.empty();
        return values.stream();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (isUpdated()) return updated.getAttributes();
        return cached.getAttributes(keycloakSession, modelSupplier);
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        if (isUpdated()) return updated.getRealmRoleMappingsStream();
        return getRoleMappingsStream().filter(r -> RoleUtils.isRealmRole(r, realm));
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        if (isUpdated()) return updated.getClientRoleMappingsStream(app);
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public boolean hasDirectRole(RoleModel role) {
        if (isUpdated()) return updated.hasDirectRole(role);

        return cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        if (isUpdated()) return updated.hasRole(role);
        if (cached.getRoleMappings(keycloakSession, modelSupplier).contains(role.getId())) return true;
        if (getRoleMappingsStream().anyMatch(r -> r.hasRole(role))) return true;
        GroupModel parent = getParent();
        return parent != null && parent.hasRole(role);
    }

    @Override
    public void grantRole(RoleModel role) {
        getDelegateForUpdate();
        updated.grantRole(role);
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        if (isUpdated()) return updated.getRoleMappingsStream();
        Set<RoleModel> roles = new HashSet<>();
        for (String id : cached.getRoleMappings(keycloakSession, modelSupplier)) {
            RoleModel roleById = keycloakSession.roles().getRoleById(realm, id);
            if (roleById == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getRoleMappingsStream();
            }
            roles.add(roleById);

        }
        return roles.stream();
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
        return keycloakSession.groups().getGroupById(realm, cached.getParentId());
    }

    @Override
    public String getParentId() {
        if (isUpdated()) return updated.getParentId();
        return cached.getParentId();
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        if (isUpdated()) return updated.getSubGroupsStream();
        Set<GroupModel> subGroups = new HashSet<>();
        for (String id : cached.getSubGroups(keycloakSession, modelSupplier)) {
            GroupModel subGroup = keycloakSession.groups().getGroupById(realm, id);
            if (subGroup == null) {
                // chance that role was removed, so just delegate to persistence and get user invalidated
                getDelegateForUpdate();
                return updated.getSubGroupsStream();

            }
            subGroups.add(subGroup);
        }
        return subGroups.stream().sorted(GroupModel.COMPARE_BY_NAME);
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream(String search, Integer firstResult, Integer maxResults) {
        if (isUpdated()) return updated.getSubGroupsStream(search, firstResult, maxResults);
        return modelSupplier.get().getSubGroupsStream(search, firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream(Integer firstResult, Integer maxResults) {
        if (isUpdated()) return updated.getSubGroupsStream(firstResult, maxResults);
        return modelSupplier.get().getSubGroupsStream(firstResult, maxResults);
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream(String search, Boolean exact, Integer firstResult, Integer maxResults) {
        if (isUpdated()) return updated.getSubGroupsStream(search, exact, firstResult, maxResults);
        return modelSupplier.get().getSubGroupsStream(search, exact, firstResult, maxResults);
    }

    @Override
    public Long getSubGroupsCount() {
        if (isUpdated()) return updated.getSubGroupsCount();
        GroupModel model = modelSupplier.get();
        return model == null ? null : model.getSubGroupsCount();
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
        return cacheSession.getGroupDelegate().getGroupById(realm, cached.getId());
    }

    @Override
    public boolean escapeSlashesInGroupPath() {
        return KeycloakModelUtils.escapeSlashesInGroupPath(keycloakSession);
    }

    @Override
    public Type getType() {
        if (isUpdated()) return updated.getType();
        return cached.getType();
    }
}
