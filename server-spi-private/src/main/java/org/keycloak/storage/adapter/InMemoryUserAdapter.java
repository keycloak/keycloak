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
import org.keycloak.models.UserModelDefaultMethods;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.ReadOnlyException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class InMemoryUserAdapter extends UserModelDefaultMethods.Streams {
    private Long createdTimestamp = Time.currentTimeMillis();
    private boolean emailVerified;
    private boolean enabled;

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

    @Override
    public String getUsername() {
        return getFirstAttribute(UserModel.USERNAME);
    }

    @Override
    public void setUsername(String username) {
        username = username==null ? null : username.toLowerCase();
        setSingleAttribute(UserModel.USERNAME, username);
    }

    public void addDefaults() {
        this.grantRole(realm.getDefaultRole());

        realm.getDefaultGroupsStream().forEach(this::joinGroup);
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
        if (UserModel.USERNAME.equals(name) || UserModel.EMAIL.equals(name)) {
            value = KeycloakModelUtils.toLowerCaseSafe(value);
        }
        attributes.putSingle(name, value);

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        checkReadonly();
        if (UserModel.USERNAME.equals(name) || UserModel.EMAIL.equals(name)) {
            String lowerCasedFirstValue = KeycloakModelUtils.toLowerCaseSafe((values != null && values.size() > 0) ? values.get(0) : null);
            if (lowerCasedFirstValue != null) values.set(0, lowerCasedFirstValue);
        }
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
    public Stream<String> getAttributeStream(String name) {
        List<String> value = this.attributes.get(name);
        return value != null ? value.stream() : Stream.empty();
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return this.requiredActions.stream();
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
    public boolean isEmailVerified() {
        return emailVerified;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        checkReadonly();
        this.emailVerified = verified;

    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return groupIds.stream().map(realm::getGroupById);
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
        return RoleUtils.isMember(getGroupsStream(), group);
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
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream().filter(RoleUtils::isRealmRole);
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return RoleUtils.hasRole(getRoleMappingsStream(), role)
                || RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        roleIds.add(role.getId());

    }

    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        return roleIds.stream().map(realm::getRoleById);
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
