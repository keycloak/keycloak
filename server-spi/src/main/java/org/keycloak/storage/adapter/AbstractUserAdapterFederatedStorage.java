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
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModelDefaultMethods;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return session.userFederatedStorage();
    }

    @Override
    public Set<String> getRequiredActions() {
        return getFederatedStorage().getRequiredActions(realm, this.getId());
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
     *
     * @return
     */
    @Override
    public Set<GroupModel> getGroups() {
        Set<GroupModel> set = new HashSet<>(getFederatedStorage().getGroups(realm, this.getId()));
        if (appendDefaultGroups()) set.addAll(realm.getDefaultGroupsStream().collect(Collectors.toSet()));
        set.addAll(getGroupsInternal());
        return set;
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
        return RoleUtils.isMember(getGroups().stream(), group);
    }

    /**
     * Gets role mappings from federated storage and automatically appends default roles.
     * Also calls getRoleMappingsInternal() method
     * to pull role mappings from provider.  Implementors can override that method
     *
     *
     * @return
     */
    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return this.getRoleMappings().stream().filter(RoleUtils::isRealmRole).collect(Collectors.toSet());
    }

    /**
     * Gets role mappings from federated storage and automatically appends default roles.
     * Also calls getRoleMappingsInternal() method
     * to pull role mappings from provider.  Implementors can override that method
     *
     *
     * @return
     */
    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        return getRoleMappings().stream().filter(r -> RoleUtils.isClientRole(r, app)).collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return RoleUtils.hasRole(getRoleMappings().stream(), role)
          || RoleUtils.hasRoleFromGroup(getGroups().stream(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
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
     *
     * @return
     */
    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<RoleModel> set = new HashSet<>(getFederatedRoleMappings());
        if (appendDefaultRolesToRoleMappings()) set.addAll(realm.getDefaultRole().getCompositesStream().collect(Collectors.toSet()));
        set.addAll(getRoleMappingsInternal());
        return set;
    }

    protected Set<RoleModel> getFederatedRoleMappings() {
        return getFederatedStorage().getRoleMappings(realm, this.getId());
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
            setUsername((values != null && values.size() > 0) ? values.get(0) : null);
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
    public List<String> getAttribute(String name) {
        if (UserModel.USERNAME.equals(name)) {
            return Collections.singletonList(getUsername());
        }
        List<String> result = getFederatedStorage().getAttributes(realm, this.getId()).get(mapAttribute(name));
        return (result == null) ? Collections.emptyList() : result;
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
     * The {@link AbstractUserAdapterFederatedStorage.Streams} class extends the {@link AbstractUserAdapterFederatedStorage}
     * abstract class and implements the {@link UserModel.Streams} interface, allowing subclasses to focus on the implementation
     * of the {@link Stream}-based query methods and providing default implementations for the collections-based variants
     * that delegate to their {@link Stream} counterparts.
     */
    public abstract static class Streams extends AbstractUserAdapterFederatedStorage implements UserModel.Streams {

        public Streams(final KeycloakSession session, final RealmModel realm, final ComponentModel storageProviderModel) {
            super(session, realm, storageProviderModel);
        }

        // user-related methods.

        @Override
        public Set<String> getRequiredActions() {
            return this.getRequiredActionsStream().collect(Collectors.toSet());
        }

        @Override
        public Stream<String> getRequiredActionsStream() {
            return super.getFederatedStorage().getRequiredActionsStream(super.realm, super.getId());
        }

        @Override
        public List<String> getAttribute(String name) {
            return this.getAttributeStream(name).collect(Collectors.toList());
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            if (UserModel.USERNAME.equals(name)) {
                return Stream.of(getUsername());
            }
            List<String> result = super.getFederatedStorage().getAttributes(realm, this.getId()).get(super.mapAttribute(name));
            return (result == null) ? Stream.empty() : result.stream();
        }

        // group-related methods.

        @Override
        public Set<GroupModel> getGroups() {
            return this.getGroupsStream().collect(Collectors.toSet());
        }

        @Override
        public Stream<GroupModel> getGroupsStream() {
            Stream<GroupModel> groups = getFederatedStorage().getGroupsStream(realm, this.getId());
            if (appendDefaultGroups()) groups = Stream.concat(groups, realm.getDefaultGroupsStream());
            return Stream.concat(groups, getGroupsInternal().stream());
        }

        @Override
        public boolean isMemberOf(GroupModel group) {
            return RoleUtils.isMember(this.getGroupsStream(), group);
        }

        // role-related methods.

        @Override
        public Set<RoleModel> getRealmRoleMappings() {
            return this.getRealmRoleMappingsStream().collect(Collectors.toSet());
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            return getRoleMappingsStream().filter(RoleUtils::isRealmRole);
        }

        @Override
        public Set<RoleModel> getClientRoleMappings(ClientModel app) {
            return this.getClientRoleMappingsStream(app).collect(Collectors.toSet());
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
            return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
        }

        @Override
        public Set<RoleModel> getRoleMappings() {
            return this.getRoleMappingsStream().collect(Collectors.toSet());
        }

        @Override
        public Stream<RoleModel> getRoleMappingsStream() {
            Stream<RoleModel> roleMappings = getFederatedRoleMappings().stream();
            if (appendDefaultRolesToRoleMappings()) roleMappings = Stream.concat(roleMappings, realm.getDefaultRole().getCompositesStream());
            return Stream.concat(roleMappings, getRoleMappingsInternal().stream());
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return RoleUtils.hasRole(this.getRoleMappingsStream(), role)
                    || RoleUtils.hasRoleFromGroup(this.getGroupsStream(), role, true);
        }
    }
}
