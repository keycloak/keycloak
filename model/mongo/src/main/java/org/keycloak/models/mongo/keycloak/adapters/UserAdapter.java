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

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around UserData object, which will persist wrapped object after each set operation (compatibility with picketlink based idm)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAdapter extends AbstractMongoAdapter<MongoUserEntity> implements UserModel {
    
    private final MongoUserEntity user;
    private final RealmModel realm;
    private final KeycloakSession session;

    public UserAdapter(KeycloakSession session, RealmModel realm, MongoUserEntity userEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.user = userEntity;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        username = KeycloakModelUtils.toLowerCaseSafe(username);

        user.setUsername(username);
        updateUser();
    }

    @Override
    public Long getCreatedTimestamp() {
        return user.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        user.setCreatedTimestamp(timestamp);
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
        updateUser();
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
        updateUser();
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
        updateUser();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        email = KeycloakModelUtils.toLowerCaseSafe(email);

        user.setEmail(email);
        updateUser();
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
        updateUser();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        List<String> attrValues = new ArrayList<>();
        attrValues.add(value);
        user.getAttributes().put(name, attrValues);
        updateUser();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        user.getAttributes().put(name, values);
        updateUser();
    }

    @Override
    public void removeAttribute(String name) {
        if (user.getAttributes() == null) return;

        user.getAttributes().remove(name);
        updateUser();
    }

    @Override
    public String getFirstAttribute(String name) {
        if (user.getAttributes()==null) return null;

        List<String> attrValues = user.getAttributes().get(name);
        return (attrValues==null || attrValues.isEmpty()) ? null : attrValues.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (user.getAttributes()==null) return Collections.<String>emptyList();
        List<String> attrValues = user.getAttributes().get(name);
        return (attrValues == null) ? Collections.<String>emptyList() : Collections.unmodifiableList(attrValues);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return user.getAttributes()==null ? Collections.<String, List<String>>emptyMap() : Collections.unmodifiableMap((Map) user.getAttributes());
    }

    public MongoUserEntity getUser() {
        return user;
    }


    @Override
    public Set<String> getRequiredActions() {
        Set<String> result = new HashSet<String>();
        if (user.getRequiredActions() != null) {
            result.addAll(user.getRequiredActions());
        }
        return result;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        String actionName = action.name();
        addRequiredAction(actionName);
    }

    @Override
    public void addRequiredAction(String actionName) {
        getMongoStore().pushItemToList(user, "requiredActions", actionName, true, invocationContext);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        String actionName = action.name();
        removeRequiredAction(actionName);
    }

    @Override
    public void removeRequiredAction(String actionName) {
        getMongoStore().pullItemFromList(user, "requiredActions", actionName, invocationContext);
    }

    protected void updateUser() {
        super.updateMongoEntity();
    }

    @Override
    public MongoUserEntity getMongoEntity() {
        return user;
    }

    @Override
    public Set<GroupModel> getGroups() {
        if (user.getGroupIds() == null || user.getGroupIds().size() == 0) return Collections.EMPTY_SET;
        Set<GroupModel> groups = new HashSet<>();
        for (String id : user.getGroupIds()) {
            groups.add(realm.getGroupById(id));
        }
        return groups;
    }

    @Override
    public void joinGroup(GroupModel group) {
        getMongoStore().pushItemToList(getUser(), "groupIds", group.getId(), true, invocationContext);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        if (user == null || group == null) return;

        getMongoStore().pullItemFromList(getUser(), "groupIds", group.getId(), invocationContext);

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        if (user.getGroupIds() == null) return false;
        if (user.getGroupIds().contains(group.getId())) return true;
        Set<GroupModel> groups = getGroups();
        return RoleUtils.isMember(groups, group);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return RoleUtils.hasRole(roles, role)
          || RoleUtils.hasRoleFromGroup(getGroups(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        getMongoStore().pushItemToList(getUser(), "roleIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        List<RoleModel> roles = MongoModelUtils.getAllRolesOfUser(realm, this);
        return new HashSet<RoleModel>(roles);
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
        if (user == null || role == null) return;

        getMongoStore().pullItemFromList(getUser(), "roleIds", role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleModel> roles = MongoModelUtils.getAllRolesOfUser(realm, this);

        for (RoleModel role : roles) {
            if (app.equals(role.getContainer())) {
                result.add(role);
            }
        }
        return result;
    }

    @Override
    public String getFederationLink() {
        return user.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        user.setFederationLink(link);
        updateUser();
    }

    @Override
    public String getServiceAccountClientLink() {
        return user.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        user.setServiceAccountClientLink(clientInternalId);
        updateUser();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
