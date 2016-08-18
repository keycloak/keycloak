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

import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Assumes everything is managed by federated storage except for username.  getId() returns a default value
 * of "f:" + providerId + ":" + getUsername().  UserModel properties like enabled, firstName, lastName, email, etc. are all
 * stored as attributes in federated storage.
 *
 * isEnabled() defaults to true if the ENABLED_ATTRIBUTE isn't set in federated
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUserAdapterFederatedStorage implements UserModel {
    public static String FIRST_NAME_ATTRIBUTE = "FIRST_NAME";
    public static String LAST_NAME_ATTRIBUTE = "LAST_NAME";
    public static String EMAIL_ATTRIBUTE = "EMAIL";
    public static String EMAIL_VERIFIED_ATTRIBUTE = "EMAIL_VERIFIED";
    public static String CREATED_TIMESTAMP_ATTRIBUTE = "CREATED_TIMESTAMP";
    public static String ENABLED_ATTRIBUTE = "ENABLED";
    public static String OTP_ENABLED_ATTRIBUTE = "OTP_ENABLED";


    protected KeycloakSession session;
    protected RealmModel realm;
    protected ComponentModel storageProviderModel;

    public AbstractUserAdapterFederatedStorage(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel) {
        this.session = session;
        this.realm = realm;
        this.storageProviderModel = storageProviderModel;
    }

    public UserFederatedStorageProvider getFederatedStorage() {
        return session.userFederatedStorage();
    }

    @Override
    public Set<String> getRequiredActions() {
        return getFederatedStorage().getRequiredActions(realm, this);
    }

    @Override
    public void addRequiredAction(String action) {
        getFederatedStorage().addRequiredAction(realm, this, action);

    }

    @Override
    public void removeRequiredAction(String action) {
        getFederatedStorage().removeRequiredAction(realm, this, action);

    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getFederatedStorage().addRequiredAction(realm, this, action.name());

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getFederatedStorage().removeRequiredAction(realm, this, action.name());
    }

    /**
     * Get group membership mappings that are managed by this storage provider
     *
     * @return
     */
    protected Set<GroupModel> getGroupsInternal() {
        return Collections.EMPTY_SET;
    }

    /**
     * Should the realm's default groups be appended to getGroups() call?
     * If your storage provider is not managing group mappings then it is recommended that
     * this method return true
     *
     * @return
     */
    protected boolean appendDefaultGroups() {
        return true;
    }

    @Override
    public Set<GroupModel> getGroups() {
        Set<GroupModel> set = new HashSet<>();
        set.addAll(getFederatedStorage().getGroups(realm, this));
        if (appendDefaultGroups()) set.addAll(realm.getDefaultGroups());
        set.addAll(getGroupsInternal());
        return set;
    }

    @Override
    public void joinGroup(GroupModel group) {
        getFederatedStorage().joinGroup(realm, this, group);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        getFederatedStorage().leaveGroup(realm, this, group);

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        Set<GroupModel> roles = getGroups();
        return KeycloakModelUtils.isMember(roles, group);
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
                ClientModel appModel = (ClientModel) container;
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
        getFederatedStorage().grantRole(realm, this, role);

    }

    /**
     * Should the realm's default roles be appended to getRoleMappings() call?
     * If your storage provider is not managing all role mappings then it is recommended that
     * this method return true
     *
     * @return
     */
    protected boolean appendDefaultRolesToRoleMappings() {
        return true;
    }

    protected Set<RoleModel> getRoleMappingsInternal() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<RoleModel> set = new HashSet<>();
        set.addAll(getFederatedRoleMappings());
        if (appendDefaultRolesToRoleMappings()) set.addAll(DefaultRoles.getDefaultRoles(realm));
        set.addAll(getRoleMappingsInternal());
        return set;
    }

    protected Set<RoleModel> getFederatedRoleMappings() {
        return getFederatedStorage().getRoleMappings(realm, this);
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getFederatedStorage().deleteRoleMapping(realm, this, role);

    }

    @Override
    public boolean isEnabled() {
        String val = getFirstAttribute(ENABLED_ATTRIBUTE);
        if (val == null) return true;
        else return Boolean.valueOf(val);
    }

    @Override
    public void setEnabled(boolean enabled) {
       setSingleAttribute(ENABLED_ATTRIBUTE, Boolean.toString(enabled));
    }

    @Override
    public boolean isOtpEnabled() {
        String val = getFirstAttribute(OTP_ENABLED_ATTRIBUTE);
        if (val == null) return false;
        else return Boolean.valueOf(val);
    }

    @Override
    public void setOtpEnabled(boolean totp) {
        setSingleAttribute(OTP_ENABLED_ATTRIBUTE, Boolean.toString(totp));

    }

    /**
     * This method should not be overriden
     *
     * @return
     */
    @Override
    public String getFederationLink() {
        return null;
    }

    /**
     * This method should not be overriden
     *
     * @return
     */
    @Override
    public void setFederationLink(String link) {

    }

    /**
     * This method should not be overriden
     *
     * @return
     */
    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    /**
     * This method should not be overriden
     *
     * @return
     */
    @Override
    public void setServiceAccountClientLink(String clientInternalId) {

    }

    protected StorageId storageId;

    /**
     * Defaults to 'f:' + storageProvider.getId() + ':' + getUsername()
     *
     * @return
     */
    @Override
    public String getId() {
        if (storageId == null) {
            storageId = new StorageId(storageProviderModel.getId(), getUsername());
        }
        return storageId.getId();
    }

    @Override
    public Long getCreatedTimestamp() {
        String val = getFirstAttribute(CREATED_TIMESTAMP_ATTRIBUTE);
        if (val == null) return null;
        else return Long.valueOf(val);
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        if (timestamp == null) {
            setSingleAttribute(CREATED_TIMESTAMP_ATTRIBUTE, null);
        } else {
            setSingleAttribute(CREATED_TIMESTAMP_ATTRIBUTE, Long.toString(timestamp));
        }

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        getFederatedStorage().setSingleAttribute(realm, this, name, value);

    }

    @Override
    public void removeAttribute(String name) {
        getFederatedStorage().removeAttribute(realm, this, name);

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getFederatedStorage().setAttribute(realm, this, name, values);

    }

    @Override
    public String getFirstAttribute(String name) {
        return getFederatedStorage().getAttributes(realm, this).getFirst(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return getFederatedStorage().getAttributes(realm, this);
    }

    @Override
    public List<String> getAttribute(String name) {
        return getFederatedStorage().getAttributes(realm, this).get(name);
    }

    @Override
    public String getFirstName() {
        return getFirstAttribute(FIRST_NAME_ATTRIBUTE);
    }

    @Override
    public void setFirstName(String firstName) {
        setSingleAttribute(FIRST_NAME_ATTRIBUTE, firstName);

    }

    @Override
    public String getLastName() {
        return getFirstAttribute(LAST_NAME_ATTRIBUTE);
    }

    @Override
    public void setLastName(String lastName) {
        setSingleAttribute(LAST_NAME_ATTRIBUTE, lastName);

    }

    @Override
    public String getEmail() {
        return getFirstAttribute(EMAIL_ATTRIBUTE);
    }

    @Override
    public void setEmail(String email) {
        setSingleAttribute(EMAIL_ATTRIBUTE, email);

    }

    @Override
    public boolean isEmailVerified() {
        String val = getFirstAttribute(EMAIL_VERIFIED_ATTRIBUTE);
        if (val == null) return false;
        else return Boolean.valueOf(val);
    }

    @Override
    public void setEmailVerified(boolean verified) {
        setSingleAttribute(EMAIL_VERIFIED_ATTRIBUTE, Boolean.toString(verified));

    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        getFederatedStorage().updateCredential(realm, this, cred);

    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return getFederatedStorage().getCredentials(realm, this);
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        getFederatedStorage().updateCredential(realm, this, cred);

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
