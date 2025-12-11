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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModelDefaultMethods;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageUtil;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

/**
 * Assumes everything is managed by federated storage except for username.  getId() returns a default value
 * of "f:" + providerId + ":" + getUsername().  UserModel properties like enabled, firstName, lastName, email, etc. are all
 * stored as attributes in federated storage.
 *
 * isEnabled() defaults to true if the ENABLED_ATTRIBUTE isn't set in federated storage
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUserAdapterFederatedStorage extends UserModelDefaultMethods {
    public static String FIRST_NAME_ATTRIBUTE = "FIRST_NAME";
    public static String LAST_NAME_ATTRIBUTE = "LAST_NAME";
    public static String EMAIL_ATTRIBUTE = "EMAIL";
    public static String EMAIL_VERIFIED_ATTRIBUTE = "EMAIL_VERIFIED";
    public static String CREATED_TIMESTAMP_ATTRIBUTE = "CREATED_TIMESTAMP";
    public static String ENABLED_ATTRIBUTE = "ENABLED";


    protected KeycloakSession session;
    protected RealmModel realm;
    protected ComponentModel storageProviderModel;

    public AbstractUserAdapterFederatedStorage(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel) {
        this.session = session;
        this.realm = realm;
        this.storageProviderModel = storageProviderModel;
    }

    public UserFederatedStorageProvider getFederatedStorage() {
        return UserStorageUtil.userFederatedStorage(session);
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return getFederatedStorage().getRequiredActionsStream(realm, this.getId());
    }

    @Override
    public void addRequiredAction(String action) {
        getFederatedStorage().addRequiredAction(realm, this.getId(), action);

    }

    @Override
    public void removeRequiredAction(String action) {
        getFederatedStorage().removeRequiredAction(realm, this.getId(), action);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getFederatedStorage().addRequiredAction(realm, this.getId(), action.name());

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getFederatedStorage().removeRequiredAction(realm, this.getId(), action.name());
    }

    /**
     * Get group membership mappings that are managed by this storage provider
     *
     * @return
     */
    protected Set<GroupModel> getGroupsInternal() {
        return Collections.emptySet();
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

    /**
     * Gets groups from federated storage and automatically appends default groups of realm.
     * Also calls getGroupsInternal() method
     * to pull group membership from provider.  Implementors can override that method
     *
     */
    @Override
    public Stream<GroupModel> getGroupsStream() {
        Stream<GroupModel> groups = getFederatedStorage().getGroupsStream(realm, this.getId());
        if (appendDefaultGroups()) groups = Stream.concat(groups, realm.getDefaultGroupsStream());
        return Stream.concat(groups, getGroupsInternal().stream());
    }

    @Override
    public void joinGroup(GroupModel group) {
        getFederatedStorage().joinGroup(realm, this.getId(), group);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        getFederatedStorage().leaveGroup(realm, this.getId(), group);

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return RoleUtils.isMember(getGroupsStream(), group);
    }

    /**
     * Gets role mappings from federated storage and automatically appends default roles.
     * Also calls getRoleMappingsInternal() method
     * to pull role mappings from provider.  Implementors can override that method
     */
    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return this.getRoleMappingsStream().filter(RoleUtils::isRealmRole);
    }

    /**
     * Gets role mappings from federated storage and automatically appends default roles.
     * Also calls getRoleMappingsInternal() method
     * to pull role mappings from provider.  Implementors can override that method
     */
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
        if (hasDirectRole(role)) return;
        getFederatedStorage().grantRole(realm, this.getId(), role);
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
        return Collections.emptySet();
    }

    /**
     * Gets role mappings from federated storage and automatically appends default roles.
     * Also calls getRoleMappingsInternal() method
     * to pull role mappings from provider.  Implementors can override that method
     */
    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        Stream<RoleModel> roleMappings = getFederatedRoleMappingsStream();
        if (appendDefaultRolesToRoleMappings()) {
            roleMappings = Stream.concat(roleMappings, realm.getDefaultRole().getCompositesStream());
        }
        return Stream.concat(roleMappings, getRoleMappingsInternal().stream());
    }

    /**
     * @deprecated Use {@link #getFederatedRoleMappingsStream()} instead
     */
    @Deprecated
    protected Set<RoleModel> getFederatedRoleMappings() {
        return getFederatedRoleMappingsStream().collect(Collectors.toSet());
    }

    protected Stream<RoleModel> getFederatedRoleMappingsStream() {
        return getFederatedStorage().getRoleMappingsStream(realm, this.getId());
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        getFederatedStorage().deleteRoleMapping(realm, this.getId(), role);

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

    /**
     * This method should not be overridden
     *
     * @return
     */
    @Override
    public String getFederationLink() {
        return StorageId.providerId(getId());
    }

    /**
     * This method should not be overridden
     *
     * @return
     */
    @Override
    public void setFederationLink(String link) {

    }

    /**
     * This method should not be overridden
     *
     * @return
     */
    @Override
    public String getServiceAccountClientLink() {
        return null;
    }

    /**
     * This method should not be overridden
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
        if (UserModel.USERNAME.equals(name)) {
            setUsername(value);
        } else {
            getFederatedStorage().setSingleAttribute(realm, this.getId(), mapAttribute(name), value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        getFederatedStorage().removeAttribute(realm, this.getId(), name);

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (UserModel.USERNAME.equals(name)) {
            setUsername((values != null && !values.isEmpty()) ? values.get(0) : null);
        } else {
            getFederatedStorage().setAttribute(realm, this.getId(), mapAttribute(name), values);
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        if (UserModel.USERNAME.equals(name)) {
            return getUsername();
        }
        return getFederatedStorage().getAttributes(realm, this.getId()).getFirst(mapAttribute(name));
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> attributes = getFederatedStorage().getAttributes(realm, this.getId());
        if (attributes == null) {
            attributes = new MultivaluedHashMap<>();
        }
        List<String> firstName = attributes.remove(FIRST_NAME_ATTRIBUTE);
        attributes.add(UserModel.FIRST_NAME, firstName != null && firstName.size() >= 1 ? firstName.get(0) : null);
        List<String> lastName = attributes.remove(LAST_NAME_ATTRIBUTE);
        attributes.add(UserModel.LAST_NAME, lastName != null && lastName.size() >= 1 ? lastName.get(0) : null);
        List<String> email = attributes.remove(EMAIL_ATTRIBUTE);
        attributes.add(UserModel.EMAIL, email != null && email.size() >= 1 ? email.get(0) : null);
        attributes.add(UserModel.USERNAME, getUsername());
        return attributes;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (UserModel.USERNAME.equals(name)) {
            return Stream.of(getUsername());
        }
        List<String> result = getFederatedStorage().getAttributes(realm, this.getId()).get(mapAttribute(name));
        return (result == null) ? Stream.empty() : result.stream();
    }

    protected String mapAttribute(String attributeName) {
        if (UserModel.FIRST_NAME.equals(attributeName)) {
            return FIRST_NAME_ATTRIBUTE;
        } else if (UserModel.LAST_NAME.equals(attributeName)) {
            return LAST_NAME_ATTRIBUTE;
        } else if (UserModel.EMAIL.equals(attributeName)) {
            return EMAIL_ATTRIBUTE;
        }
        return attributeName;
    }

    @Override
    public boolean isEmailVerified() {
        String val = getFirstAttribute(EMAIL_VERIFIED_ATTRIBUTE);
        if (val == null) return false;
        else return Boolean.valueOf(val);
    }

    /**
     * Stores as attribute in federated storage.
     * EMAIL_VERIFIED_ATTRIBUTE
     *
     * @param verified
     */
    @Override
    public void setEmailVerified(boolean verified) {
        setSingleAttribute(EMAIL_VERIFIED_ATTRIBUTE, Boolean.toString(verified));

    }

    @Override
    public SubjectCredentialManager credentialManager() {
        return session.users().getUserCredentialManager(this);
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

    /**
     * @deprecated This interface is no longer necessary; collection-based methods were removed from the parent interface
     * and therefore the parent interface can be used directly
     */
    @Deprecated
    public abstract static class Streams extends AbstractUserAdapterFederatedStorage implements UserModel {

        public Streams(final KeycloakSession session, final RealmModel realm, final ComponentModel storageProviderModel) {
            super(session, realm, storageProviderModel);
        }
    }
}
