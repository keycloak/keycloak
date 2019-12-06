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
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This abstract class provides implementations for everything but getUsername().  getId() returns a default value
 * of "f:" + providerId + ":" + getUsername().  isEnabled() returns true.  getRoleMappings() will return default roles.
 * getGroups() will return default groups.
 *
 * All other read methods return null, an empty collection, or false depending
 * on the type.  All update methods throw a ReadOnlyException.
 *
 * Provider implementors should override the methods for attributes, properties, and mappings they support.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUserAdapter implements UserModel {
    protected KeycloakSession session;
    protected RealmModel realm;
    protected ComponentModel storageProviderModel;

    public AbstractUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel) {
        this.session = session;
        this.realm = realm;
        this.storageProviderModel = storageProviderModel;
    }

    @Override
    public Set<String> getRequiredActions() {
        return Collections.emptySet();
    }

    @Override
    public void addRequiredAction(String action) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void removeRequiredAction(String action) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        throw new ReadOnlyException("user is read only for this update");
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

    @Override
    public Set<GroupModel> getGroups() {
        Set<GroupModel> set = new HashSet<>();
        if (appendDefaultGroups()) set.addAll(realm.getDefaultGroups());
        set.addAll(getGroupsInternal());
        return set;
    }

    @Override
    public void joinGroup(GroupModel group) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void leaveGroup(GroupModel group) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        Set<GroupModel> roles = getGroups();
        return RoleUtils.isMember(roles, group);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();

        Set<RoleModel> realmRoles = new HashSet<>();
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

        Set<RoleModel> roles = new HashSet<>();
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
        return RoleUtils.hasRole(roles, role)
          || RoleUtils.hasRoleFromGroup(getGroups(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        throw new ReadOnlyException("user is read only for this update");

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

    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<RoleModel> set = new HashSet<>();
        if (appendDefaultRolesToRoleMappings()) set.addAll(DefaultRoles.getDefaultRoles(realm));
        set.addAll(getRoleMappingsInternal());
        return set;
    }


    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException("user is read only for this update");
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
        throw new ReadOnlyException("user is read only for this update");

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
        throw new ReadOnlyException("user is read only for this update");

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
    public void setUsername(String username) {
        throw new ReadOnlyException("user is read only for this update");
    }

    protected long created = System.currentTimeMillis();

    @Override
    public Long getCreatedTimestamp() {
        return created;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void setSingleAttribute(String name, String value) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void setAttribute(String name, List<String> values) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public String getFirstAttribute(String name) {
        return null;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return new MultivaluedHashMap<>();
    }

    @Override
    public List<String> getAttribute(String name) {
        return null;
    }

    @Override
    public String getFirstName() {
        return null;
    }

    @Override
    public void setFirstName(String firstName) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public String getLastName() {
        return null;
    }

    @Override
    public void setLastName(String lastName) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public boolean isEmailVerified() {
        return false;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        throw new ReadOnlyException("user is read only for this update");

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
