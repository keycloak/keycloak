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
package org.keycloak.storage.changeset;

import org.keycloak.common.util.Time;
import org.keycloak.hash.PasswordHashManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserDataAdapter implements UserModel {
    protected UserData userData;
    protected RealmModel realm;
    protected KeycloakSession session;
    protected Set<String> managedCredentialTypes;
    protected List<UserCredentialModel> updatedManagedCredentials = new LinkedList<>();

    public UserDataAdapter(KeycloakSession session, RealmModel realm, UserData userData) {
        this.session = session;
        this.realm = realm;
        this.userData = userData;
        this.userData.rememberState();
    }

    @Override
    public String getId() {
        return userData.getId();
    }

    @Override
    public String getUsername() {
        return userData.getUsername();
    }

    @Override
    public void setUsername(String username) {
        userData.setUsername(username);

    }

    @Override
    public Long getCreatedTimestamp() {
        return userData.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        userData.setCreatedTimestamp(timestamp);

    }

    @Override
    public boolean isEnabled() {
        return userData.isEnabled();
    }

    @Override
    public boolean isOtpEnabled() {
        return userData.isTotp();
    }

    @Override
    public void setEnabled(boolean enabled) {
        userData.setEnabled(enabled);

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        userData.setSingleAttribute(name, value);

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        userData.setAttribute(name, values);

    }

    @Override
    public void removeAttribute(String name) {
        userData.removeAttribute(name);

    }

    @Override
    public String getFirstAttribute(String name) {
        return userData.getAttributes().getFirst(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        return userData.getAttributes().get(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return userData.getAttributes();
    }

    @Override
    public Set<String> getRequiredActions() {
        return userData.getRequiredActions();
    }

    @Override
    public void addRequiredAction(String action) {
        userData.addRequiredAction(action);

    }

    @Override
    public void removeRequiredAction(String action) {
        userData.removeRequiredAction(action);

    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        userData.addRequiredAction(action.name());

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        userData.removeRequiredAction(action.name());

    }

    @Override
    public String getFirstName() {
        return userData.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        userData.setFirstName(firstName);

    }

    @Override
    public String getLastName() {
        return userData.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        userData.setLastName(lastName);

    }

    @Override
    public String getEmail() {
        return userData.getEmail();
    }

    @Override
    public void setEmail(String email) {
        userData.setEmail(email);

    }

    @Override
    public boolean isEmailVerified() {
        return userData.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        userData.setEmailVerified(verified);

    }

    @Override
    public void setOtpEnabled(boolean totp) {
        userData.setTotp(totp);

    }

    @Override
    public void updateCredential(UserCredentialModel cred) {

    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return null;
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {

    }

    @Override
    public Set<GroupModel> getGroups() {
        Set<String> groups = userData.getGroupIds();
        Set<GroupModel> set = new HashSet<>();
        for (String id : groups) {
            GroupModel group = realm.getGroupById(id);
            if (group != null) set.add(group);
        }
        return set;
    }

    @Override
    public void joinGroup(GroupModel group) {
        userData.joinGroup(group.getId());

    }

    @Override
    public void leaveGroup(GroupModel group) {
        userData.leaveGroup(group.getId());

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        Set<GroupModel> roles = getGroups();
        return KeycloakModelUtils.isMember(roles, group);
    }

    @Override
    public String getFederationLink() {
        return null;
    }

    @Override
    public void setFederationLink(String link) {

    }

    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {

    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> roles = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                ClientModel appModel = (ClientModel)container;
                if (appModel.getId().equals(app.getId())) {
                    roles.add(role);
                }
            }
        }
        return roles;
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return KeycloakModelUtils.hasRole(roles, role);
    }

    @Override
    public void grantRole(RoleModel role) {
        userData.grantRole(role.getId());

    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<String> roles = userData.getRoleMappings();
        Set<RoleModel> set = new HashSet<>();
        for (String id : roles) {
            RoleModel role = realm.getRoleById(id);
            if (role != null) set.add(role);
        }
        return set;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        userData.deleteRoleMapping(role.getId());
    }
}
