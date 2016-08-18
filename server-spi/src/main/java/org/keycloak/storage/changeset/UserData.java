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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.entities.AbstractIdentifiableEntity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserData {

    private String id;
    private boolean idChanged;
    private String username;
    private boolean usernameChanged;
    private Long createdTimestamp;
    private boolean createdTimestampChanged;
    private String firstName;
    private boolean firstNameChanged;
    private String lastName;
    private boolean lastNameChanged;
    private String email;
    private boolean emailChanged;
    private boolean emailVerified;
    private boolean emailVerifiedChanged;
    private boolean totp;
    private boolean totpChanged;
    private boolean enabled;
    private boolean enabledChanged;

    private Set<String> roleIds = new HashSet<>();
    private boolean rolesChanged;
    private Set<String> groupIds = new HashSet<>();
    private boolean groupsChanged;

    private MultivaluedHashMap<String, String> attributes = new MultivaluedHashMap<>();
    private boolean attributesChanged;
    private Set<String> requiredActions = new HashSet<>();
    private boolean requiredActionsChanged;
    private List<UserCredentialValueModel> credentials = new LinkedList<>();
    private boolean credentialsChanged;

    public void rememberState() {
        original = new UserData();
        original.id = id;
        original.username = username;
        original.createdTimestamp = createdTimestamp;
        original.firstName = firstName;
        original.lastName = lastName;
        original.email = email;
        original.emailVerified = emailVerified;
        original.totp = totp;
        original.enabled = enabled;
        original.attributes.putAll(attributes);
        original.requiredActions.addAll(requiredActions);
        original.credentials.addAll(credentials);
    }

    private UserData original = null;

    public void clearChangeFlags() {
        original = null;
        idChanged = false;
        usernameChanged = false;
        createdTimestampChanged = false;
        firstNameChanged = false;
        lastNameChanged = false;
        emailChanged = false;
        emailVerifiedChanged = false;
        totpChanged = false;
        enabledChanged = false;
        rolesChanged = false;
        groupsChanged = false;
        attributesChanged = false;
        requiredActionsChanged = false;
        credentialsChanged = false;
    }

    public boolean isChanged() {
        return !idChanged
        && !usernameChanged
        && !createdTimestampChanged
        && !firstNameChanged
        && !lastNameChanged
        && !emailChanged
        && !emailVerifiedChanged
        && !totpChanged
        && !enabledChanged
        && !rolesChanged
        && !groupsChanged
        && !attributesChanged
        && !requiredActionsChanged
        && !credentialsChanged;
    }

    public boolean isIdChanged() {
        return idChanged;
    }

    public boolean isUsernameChanged() {
        return usernameChanged;
    }

    public boolean isCreatedTimestampChanged() {
        return createdTimestampChanged;
    }

    public boolean isFirstNameChanged() {
        return firstNameChanged;
    }

    public boolean isLastNameChanged() {
        return lastNameChanged;
    }

    public boolean isEmailChanged() {
        return emailChanged;
    }

    public boolean isEmailVerifiedChanged() {
        return emailVerifiedChanged;
    }

    public boolean isTotpChanged() {
        return totpChanged;
    }

    public boolean isEnabledChanged() {
        return enabledChanged;
    }

    public boolean isRolesChanged() {
        return rolesChanged;
    }

    public boolean isGroupsChanged() {
        return groupsChanged;
    }

    public boolean isAttributesChanged() {
        return attributesChanged;
    }

    public boolean isRequiredActionsChanged() {
        return requiredActionsChanged;
    }

    public boolean isCredentialsChanged() {
        return credentialsChanged;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        idChanged = true;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        usernameChanged = true;
    }
    
    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long timestamp) {
        this.createdTimestamp = timestamp;
        createdTimestampChanged = true;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        firstNameChanged = true;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        lastNameChanged = true;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        emailChanged = true;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
        emailVerifiedChanged = true;
    }

    public boolean isTotp() {
        return totp;
    }

    public void setTotp(boolean totp) {
        this.totp = totp;
        totpChanged = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        enabledChanged = true;
    }

    public Set<String> getRoleMappings() {
        return Collections.unmodifiableSet(roleIds);
    }

    public void grantRole(String roleId) {
        if (roleIds.contains(roleId)) return;
        roleIds.add(roleId);
        rolesChanged = true;
    }

    public void deleteRoleMapping(String roleId) {
        if (!roleIds.contains(roleId)) return;
        roleIds.remove(roleId);
        rolesChanged = true;
    }

    public MultivaluedHashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setSingleAttribute(String name, String value) {
        attributes.putSingle(name, value);
        attributesChanged = true;

    }
    public void setAttribute(String name, List<String> values) {
        attributes.put(name, values);
        attributesChanged = true;
    }
    public void removeAttribute(String name) {
        attributes.remove(name);
        attributesChanged = true;
    }



    public Set<String> getRequiredActions() {
        return Collections.unmodifiableSet(requiredActions);
    }
    public void addRequiredAction(String action) {
        if (requiredActions.contains(action)) return;
        requiredActions.add(action);
        requiredActionsChanged = true;
    }
    public void removeRequiredAction(String action) {
        if (!requiredActions.contains(action)) return;
        requiredActions.remove(action);
        requiredActionsChanged = true;
    }

    public List<UserCredentialValueModel> getCredentials() {
        return Collections.unmodifiableList(credentials);
    }

    public void removeCredentialType(String type) {
        Iterator<UserCredentialValueModel> it = credentials.iterator();
        while (it.hasNext()) {
            if (it.next().getType().equals(type)) {
                it.remove();
                credentialsChanged = true;
            }
        }

    }

    public void removeCredentialDevice(String type, String device) {
        Iterator<UserCredentialValueModel> it = credentials.iterator();
        while (it.hasNext()) {
            UserCredentialValueModel next = it.next();
            if (next.getType().equals(type) && next.getDevice().equals(device)) {
                it.remove();
                credentialsChanged = true;
            }
        }

    }

    public void setCredential(UserCredentialValueModel cred) {
        removeCredentialType(cred.getType());
        addCredential(cred);
    }
    public void addCredential(UserCredentialValueModel cred) {
        credentials.add(cred);
        credentialsChanged = true;
    }

    public Set<String> getGroupIds() {
        return Collections.unmodifiableSet(groupIds);
    }

    public void joinGroup(String groupId) {
        if (groupIds.contains(groupId)) return;
        groupIds.add(groupId);
        groupsChanged = true;
    }

    public void leaveGroup(String groupId) {
        if (!groupIds.contains(groupId)) return;
        groupIds.remove(groupId);
        groupsChanged = true;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (this.id == null) return false;

        if (o == null || getClass() != o.getClass()) return false;

        AbstractIdentifiableEntity that = (AbstractIdentifiableEntity) o;

        if (!getId().equals(that.getId())) return false;

        return true;

    }

    @Override
    public int hashCode() {
        return id!=null ? id.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [ id=%s ]", getClass().getSimpleName(), getId());
    }

}

