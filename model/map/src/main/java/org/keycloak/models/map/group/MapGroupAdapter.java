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

package org.keycloak.models.map.group;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public abstract class MapGroupAdapter<K> extends AbstractGroupModel<MapGroupEntity<K>> {
    public MapGroupAdapter(KeycloakSession session, RealmModel realm, MapGroupEntity<K> entity) {
        super(session, realm, entity);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        entity.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        List<String> attributeValues = this.entity.getAttribute(name);
        if (attributeValues == null) {
            return null;
        }

        return attributeValues.get(0);
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        List<String> attributes = entity.getAttribute(name);
        if (attributes == null || attributes.isEmpty()) return Stream.empty();
        return attributes.stream();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return entity.getAttributes();
    }

    @Override
    public GroupModel getParent() {
        String parentId = getParentId();
        if (parentId == null) {
            return null;
        }

        return session.groups().getGroupById(realm, parentId);
    }

    @Override
    public String getParentId() {
        return entity.getParentId();
    }

    @Override
    public Stream<GroupModel> getSubGroupsStream() {
        return session.groups().getGroupsStream(realm)
                .filter(groupModel -> getId().equals(groupModel.getParentId()));
    }

    @Override
    public void setParent(GroupModel group) {
        if (group == null) {
            entity.setParentId(null);
            return;
        }
        
        if (!getId().equals(group.getId())) {
            entity.setParentId(group.getId());
        }
    }

    @Override
    public void addChild(GroupModel subGroup) {
        subGroup.setParent(this);
    }

    @Override
    public void removeChild(GroupModel subGroup) {
        if (getId().equals(subGroup.getParentId())) {
            subGroup.setParent(null);
        }
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream()
                .filter(roleModel -> roleModel.getContainer() instanceof RealmModel);
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        final String clientId = app.getId();
        return getRoleMappingsStream()
                .filter(roleModel -> roleModel.getContainer() instanceof ClientModel)
                .filter(roleModel -> roleModel.getContainer().getId().equals(clientId));
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return entity.getGrantedRoles().contains(role.getId());
    }

    @Override
    public void grantRole(RoleModel role) {
        entity.addGrantedRole(role.getId());
    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return entity.getGrantedRoles().stream()
            .map(roleId -> session.roles().getRoleById(realm, roleId));
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        entity.removeRole(role.getId());
    }
}
