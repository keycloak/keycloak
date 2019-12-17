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
package org.keycloak.storage.adapter;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.ReadOnlyException;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InMemoryUserAdapter implements UserModel {
    private String username;
    private Long createdTimestamp = Time.currentTimeMillis();
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private boolean enabled;

    private String realmId;

    private Set<String> roleIds = new HashSet<>();
    private Set<String> groupIds = new HashSet<>();

    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private Set<String> requiredActions = new HashSet<>();
    private String federationLink;
    private String serviceAccountClientLink;

    private KeycloakSession session;
    private RealmModel realm;
    private String id;
    private boolean readonly;

    public InMemoryUserAdapter(KeycloakSession session, RealmModel realm, String id) {
        this.session = session;
        this.realm = realm;
        this.id = id;


    }

    public void addDefaults() {
        DefaultRoles.addDefaultRoles(realm, this);

        for (GroupModel g : realm.getDefaultGroups()) {
            joinGroup(g);
        }

    }

    public void setReadonly(boolean flag) {
        readonly = flag;
    }

    protected void checkReadonly() {
        if (readonly) throw new ReadOnlyException("In memory user model is not writable");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        checkReadonly();
        this.username = username.toLowerCase();

    }

    @Override
    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        checkReadonly();
        this.createdTimestamp = timestamp;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        checkReadonly();
        this.enabled = enabled;

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        checkReadonly();
        attributes.putSingle(name, value);

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        checkReadonly();
        attributes.put(name, values);

    }

    @Override
    public void removeAttribute(String name) {
        checkReadonly();
        attributes.remove(name);

    }

    @Override
    public String getFirstAttribute(String name) {
        return attributes.getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        List<String> value = attributes.get(name);
        if (value == null) {
            return new LinkedList<>();
        }
        return value;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    @Override
    public Set<String> getRequiredActions() {
        return requiredActions;
    }

    @Override
    public void addRequiredAction(String action) {
        checkReadonly();
        requiredActions.add(action);

    }

    @Override
    public void removeRequiredAction(String action) {
        checkReadonly();
        requiredActions.remove(action);

    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        checkReadonly();
        requiredActions.add(action.name());

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        checkReadonly();
        requiredActions.remove(action.name());
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public void setFirstName(String firstName) {
        checkReadonly();
        this.firstName = firstName;

    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setLastName(String lastName) {
        checkReadonly();
        this.lastName = lastName;

    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        checkReadonly();
        if (email != null) email = email.toLowerCase();
        this.email = email;

    }

    @Override
    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        checkReadonly();
        this.emailVerified = verified;

    }

    @Override
    public Set<GroupModel> getGroups() {
        if (groupIds.isEmpty()) return Collections.emptySet();
        Set<GroupModel> groups = new HashSet<>();
        for (String id : groupIds) {
            groups.add(realm.getGroupById(id));
        }
        return groups;
    }

    @Override
    public void joinGroup(GroupModel group) {
        checkReadonly();
        groupIds.add(group.getId());

    }

    @Override
    public void leaveGroup(GroupModel group) {
        checkReadonly();
        groupIds.remove(group.getId());

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        if (groupIds == null) return false;
        if (groupIds.contains(group.getId())) return true;
        Set<GroupModel> groups = getGroups();
        return RoleUtils.isMember(groups, group);
    }

    @Override
    public String getFederationLink() {
        return federationLink;
    }

    @Override
    public void setFederationLink(String link) {
        checkReadonly();
        this.federationLink = link;

    }

    @Override
    public String getServiceAccountClientLink() {
        return serviceAccountClientLink;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        checkReadonly();
        this.serviceAccountClientLink = clientInternalId;

    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> allRoles = getRoleMappings();

        // Filter to retrieve just realm roles
        Set<RoleModel> realmRoles = new HashSet<>();
        for (RoleModel role : allRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<>();
        Set<RoleModel> roles = getRoleMappings();

        for (RoleModel role : roles) {
            if (app.equals(role.getContainer())) {
                result.add(role);
            }
        }
        return result;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return RoleUtils.hasRole(roles, role)
                || RoleUtils.hasRoleFromGroup(getGroups(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        roleIds.add(role.getId());

    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        if (roleIds.isEmpty()) return Collections.emptySet();
        Set<RoleModel> roles = new HashSet<>();
        for (String id : roleIds) {
            roles.add(realm.getRoleById(id));
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        roleIds.remove(role.getId());

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
