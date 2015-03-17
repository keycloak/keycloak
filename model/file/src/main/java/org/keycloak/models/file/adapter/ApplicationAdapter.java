/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file.adapter;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.OAuthClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.ApplicationEntity;
import org.keycloak.models.entities.ClientEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * ApplicationModel used for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class ApplicationAdapter extends ClientAdapter implements ApplicationModel {

    private final ApplicationEntity applicationEntity;
    private final InMemoryModel inMemoryModel;

    private final Map<String, RoleAdapter> allRoles = new HashMap<String, RoleAdapter>();

    public ApplicationAdapter(KeycloakSession session, RealmModel realm, ApplicationEntity applicationEntity, ClientEntity clientEntity, InMemoryModel inMemoryModel) {
        super(session, realm, clientEntity);
        this.applicationEntity = applicationEntity;
        this.inMemoryModel = inMemoryModel;
    }

    public ApplicationEntity getApplicationEntity() {
        return applicationEntity;
    }

    @Override
    public void updateApplication() {
    }

    @Override
    public String getName() {
        return applicationEntity.getName();
    }

    @Override
    public void setName(String name) {
        if (appNameExists(name)) throw new ModelDuplicateException("Application named " + name + " already exists.");
        applicationEntity.setName(name);
    }

    private boolean appNameExists(String name) {
        for (ApplicationModel app : realm.getApplications()) {
            if (app == this) continue;
            if (app.getName().equals(name)) return true;
        }

        return false;
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return applicationEntity.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        applicationEntity.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return applicationEntity.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        applicationEntity.setManagementUrl(url);
    }

    @Override
    public void setBaseUrl(String url) {
        applicationEntity.setBaseUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return applicationEntity.getBaseUrl();
    }

    @Override
    public boolean isBearerOnly() {
        return applicationEntity.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        applicationEntity.setBearerOnly(only);
    }

    @Override
    public boolean isPublicClient() {
        return applicationEntity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        applicationEntity.setPublicClient(flag);
    }

    @Override
    public boolean isDirectGrantsOnly() {
        return false;  // applications can't be grant only
    }

    @Override
    public void setDirectGrantsOnly(boolean flag) {
        // applications can't be grant only
    }


    @Override
    public RoleAdapter getRole(String name) {
        for (RoleAdapter role : allRoles.values()) {
            if (role.getName().equals(name)) return role;
        }
        return null;
    }

    @Override
    public RoleAdapter addRole(String name) {
        return this.addRole(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleAdapter addRole(String id, String name) {
        if (roleNameExists(name)) throw new ModelDuplicateException("Role named " + name + " already exists.");
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setApplicationId(getId());

        RoleAdapter role = new RoleAdapter(getRealm(), roleEntity, this);
        allRoles.put(id, role);

        return role;
    }

    private boolean roleNameExists(String name) {
        for (RoleModel role : allRoles.values()) {
            if (role.getName().equals(name)) return true;
        }

        return false;
    }

    @Override
    public boolean removeRole(RoleModel role) {
        boolean removed = (allRoles.remove(role.getId()) != null);

        // remove application roles from users
        for (UserModel user : inMemoryModel.getUsers(realm.getId())) {
            user.deleteRoleMapping(role);
        }

        // delete scope mappings from applications
        for (ApplicationModel app : realm.getApplications()) {
            app.deleteScopeMapping(role);
        }

        // delete scope mappings from oauth clients
        for (OAuthClientModel oaClient : realm.getOAuthClients()) {
            oaClient.deleteScopeMapping(role);
        }

        // remove role from the realm
        realm.removeRole(role);

        this.deleteScopeMapping(role);

        return removed;
    }

    @Override
    public Set<RoleModel> getRoles() {
        return new HashSet(allRoles.values());
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (super.hasScope(role)) {
            return true;
        }
        Set<RoleModel> roles = getRoles();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public Set<RoleModel> getApplicationScopeMappings(ClientModel client) {
        Set<RoleModel> allScopes = client.getScopeMappings();

        Set<RoleModel> appRoles = new HashSet<RoleModel>();
        for (RoleModel role : allScopes) {
            RoleAdapter roleAdapter = (RoleAdapter)role;
            if (getId().equals(roleAdapter.getRoleEntity().getApplicationId())) {
                appRoles.add(role);
            }
        }
        return appRoles;
    }

    @Override
    public List<String> getDefaultRoles() {
        return applicationEntity.getDefaultRoles();
    }

    @Override
    public void addDefaultRole(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            addRole(name);
        }

        List<String> defaultRoles = getDefaultRoles();
        if (defaultRoles.contains(name)) return;

        String[] defaultRoleNames = defaultRoles.toArray(new String[defaultRoles.size() + 1]);
        defaultRoleNames[defaultRoleNames.length - 1] = name;
        updateDefaultRoles(defaultRoleNames);
    }

    @Override
    public void updateDefaultRoles(String[] defaultRoles) {
        List<String> roleNames = new ArrayList<String>();
        for (String roleName : defaultRoles) {
            RoleModel role = getRole(roleName);
            if (role == null) {
                addRole(roleName);
            }

            roleNames.add(roleName);
        }

        applicationEntity.setDefaultRoles(roleNames);
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return applicationEntity.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        applicationEntity.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return applicationEntity.getRegisteredNodes() == null ? Collections.<String, Integer>emptyMap() : Collections.unmodifiableMap(applicationEntity.getRegisteredNodes());
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        if (applicationEntity.getRegisteredNodes() == null) {
            applicationEntity.setRegisteredNodes(new HashMap<String, Integer>());
        }

        applicationEntity.getRegisteredNodes().put(nodeHost, registrationTime);
    }

    @Override
    public void unregisterNode(String nodeHost) {
        if (applicationEntity.getRegisteredNodes() == null) return;

        applicationEntity.getRegisteredNodes().remove(nodeHost);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ApplicationModel)) return false;

        ApplicationModel that = (ApplicationModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
