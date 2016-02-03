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

package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.mongo.keycloak.entities.MongoGroupEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 */
public class GroupAdapter extends AbstractMongoAdapter<MongoGroupEntity> implements GroupModel {

    private final MongoGroupEntity group;
    private RealmModel realm;
    private KeycloakSession session;

    public GroupAdapter(KeycloakSession session, RealmModel realm, MongoGroupEntity group, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.group = group;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getId() {
        return group.getId();
    }

    @Override
    public String getName() {
        return group.getName();
    }

    @Override
    public void setName(String name) {
        group.setName(name);
        updateGroup();
    }

    protected void updateGroup() {
        super.updateMongoEntity();
    }

    @Override
    public MongoGroupEntity getMongoEntity() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof GroupModel)) return false;

        GroupModel that = (GroupModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (group.getAttributes() == null) {
            group.setAttributes(new HashMap<String, List<String>>());
        }

        List<String> attrValues = new ArrayList<>();
        attrValues.add(value);
        group.getAttributes().put(name, attrValues);
        updateGroup();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (group.getAttributes() == null) {
            group.setAttributes(new HashMap<String, List<String>>());
        }

        group.getAttributes().put(name, values);
        updateGroup();
    }

    @Override
    public void removeAttribute(String name) {
        if (group.getAttributes() == null) return;

        group.getAttributes().remove(name);
        updateGroup();
    }

    @Override
    public String getFirstAttribute(String name) {
        if (group.getAttributes()==null) return null;

        List<String> attrValues = group.getAttributes().get(name);
        return (attrValues==null || attrValues.isEmpty()) ? null : attrValues.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (group.getAttributes()==null) return Collections.<String>emptyList();
        List<String> attrValues = group.getAttributes().get(name);
        return (attrValues == null) ? Collections.<String>emptyList() : Collections.unmodifiableList(attrValues);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return group.getAttributes()==null ? Collections.<String, List<String>>emptyMap() : Collections.unmodifiableMap((Map) group.getAttributes());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return KeycloakModelUtils.hasRole(roles, role);
    }

    @Override
    public void grantRole(RoleModel role) {
        getMongoStore().pushItemToList(group, "roleIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (group.getRoleIds() == null || group.getRoleIds().isEmpty()) return Collections.EMPTY_SET;
        Set<RoleModel> roles = new HashSet<>();
        for (String id : group.getRoleIds()) {
            RoleModel roleById = realm.getRoleById(id);
            if (roleById == null) {
                throw new ModelException("role does not exist in group role mappings");
            }
            roles.add(roleById);
        }
        return roles;
     }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> allRoles = getRoleMappings();

        // Filter to retrieve just realm roles
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (group == null || role == null) return;

        getMongoStore().pullItemFromList(group, "roleIds", role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        Set<RoleModel> roles = getRoleMappings();

        for (RoleModel role : roles) {
            if (app.equals(role.getContainer())) {
                result.add(role);
            }
        }
        return result;
    }

    @Override
    public GroupModel getParent() {
        if (group.getParentId() == null) return null;
        return realm.getGroupById(group.getParentId());
    }

    @Override
    public String getParentId() {
        return group.getParentId();
    }

    @Override
    public Set<GroupModel> getSubGroups() {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("parentId").is(getId())
                .get();
        List<MongoGroupEntity> groups = getMongoStore().loadEntities(MongoGroupEntity.class, query, invocationContext);

        Set<GroupModel> subGroups = new HashSet<>();

        if (groups == null) return subGroups;
        for (MongoGroupEntity group : groups) {
            subGroups.add(realm.getGroupById(group.getId()));
        }

        return subGroups;
    }

    @Override
    public void setParent(GroupModel parent) {
        if (parent == null) group.setParentId(null);
        else if (parent.getId().equals(getId())) {
            return;
        }
        else {
            group.setParentId(parent.getId());
        }
        updateGroup();

    }

    @Override
    public void addChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(this);
        updateGroup();

    }

    @Override
    public void removeChild(GroupModel subGroup) {
        if (subGroup.getId().equals(getId())) {
            return;
        }
        subGroup.setParent(null);
        updateGroup();

    }
}
