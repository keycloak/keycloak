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
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.entities.FederatedIdentityEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.entities.UserEntity;

/**
 * UserModel for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class UserAdapter implements UserModel, Comparable {

    private final InMemoryModel inMemoryModel;
    private final UserEntity user;
    private final RealmModel realm;

    private final Set<RoleModel> allRoles = new HashSet<RoleModel>();

    public UserAdapter(RealmModel realm, UserEntity userEntity, InMemoryModel inMemoryModel) {
        this.user = userEntity;
        this.realm = realm;
        if (userEntity.getFederatedIdentities() == null) {
            userEntity.setFederatedIdentities(new ArrayList<FederatedIdentityEntity>());
        }
        this.inMemoryModel = inMemoryModel;
    }

    public UserEntity getUserEntity() {
        return this.user;
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
        if (getUsername() == null) {
            user.setUsername(username);
            return;
        }

        if (getUsername().equals(username)) return; // allow setting to same name

        if (inMemoryModel.hasUserWithUsername(realm.getId(), username))
            throw new ModelDuplicateException("User with username " + username + " already exists in realm.");
        user.setUsername(username);
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        if (email == null) {
            user.setEmail(email);
            return;
        }

        if (email.equals(getEmail())) return;

        RealmAdapter realmAdapter = (RealmAdapter)realm;
        if (realmAdapter.hasUserWithEmail(email)) throw new ModelDuplicateException("User with email address " + email + " already exists.");
        user.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
    }

    @Override
    public void setAttribute(String name, String value) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, String>());
        }

        user.getAttributes().put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        if (user.getAttributes() == null) return;

        user.getAttributes().remove(name);
    }

    @Override
    public String getAttribute(String name) {
        return user.getAttributes()==null ? null : user.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return user.getAttributes()==null ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(user.getAttributes());
    }

    @Override
    public Set<RequiredAction> getRequiredActions() {
        List<RequiredAction> requiredActions = user.getRequiredActions();
        if (requiredActions == null) requiredActions = new ArrayList<RequiredAction>();
        return new HashSet(requiredActions);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        List<RequiredAction> requiredActions = user.getRequiredActions();
        if (requiredActions == null) requiredActions = new ArrayList<RequiredAction>();
        if (!requiredActions.contains(action)) requiredActions.add(action);
        user.setRequiredActions(requiredActions);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        List<RequiredAction> requiredActions = user.getRequiredActions();
        if (requiredActions == null) return;
        requiredActions.remove(action);
        user.setRequiredActions(requiredActions);
    }

    @Override
    public boolean isTotp() {
        return user.isTotp();
    }

    @Override
    public void setTotp(boolean totp) {
        user.setTotp(totp);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        CredentialEntity credentialEntity = getCredentialEntity(user, cred.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(cred.getType());
            credentialEntity.setDevice(cred.getDevice());
            user.getCredentials().add(credentialEntity);
        }
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            byte[] salt = Pbkdf2PasswordEncoder.getSalt();
            int hashIterations = 1;
            PasswordPolicy policy = realm.getPasswordPolicy();
            if (policy != null) {
                hashIterations = policy.getHashIterations();
                if (hashIterations == -1) hashIterations = 1;
            }
            credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue(), hashIterations));
            credentialEntity.setSalt(salt);
            credentialEntity.setHashIterations(hashIterations);
        } else {
            credentialEntity.setValue(cred.getValue());
        }
        credentialEntity.setDevice(cred.getDevice());
    }

    private CredentialEntity getCredentialEntity(UserEntity userEntity, String credType) {
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }

        return null;
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        List<CredentialEntity> credentials = new ArrayList<CredentialEntity>(user.getCredentials());
        List<UserCredentialValueModel> result = new ArrayList<UserCredentialValueModel>();

        for (CredentialEntity credEntity : credentials) {
            UserCredentialValueModel credModel = new UserCredentialValueModel();
            credModel.setType(credEntity.getType());
            credModel.setDevice(credEntity.getDevice());
            credModel.setValue(credEntity.getValue());
            credModel.setSalt(credEntity.getSalt());
            credModel.setHashIterations(credEntity.getHashIterations());

            result.add(credModel);
        }

        return result;
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel credModel) {
        CredentialEntity credentialEntity = getCredentialEntity(user, credModel.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
        //    credentialEntity.setId(KeycloakModelUtils.generateId());
            credentialEntity.setType(credModel.getType());
        //    credentialEntity.setUser(user);
            user.getCredentials().add(credentialEntity);
        }

        credentialEntity.setValue(credModel.getValue());
        credentialEntity.setSalt(credModel.getSalt());
        credentialEntity.setDevice(credModel.getDevice());
        credentialEntity.setHashIterations(credModel.getHashIterations());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    @Override
    public void grantRole(RoleModel role) {
        allRoles.add(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        return Collections.unmodifiableSet(allRoles);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> allRoleMappings = getRoleMappings();

        // Filter to retrieve just realm roles TODO: Maybe improve to avoid filter programmatically... Maybe have separate fields for realmRoles and appRoles on user?
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allRoleMappings) {
            RoleEntity roleEntity = ((RoleAdapter) role).getRoleEntity();

            if (realm.getId().equals(roleEntity.getRealmId())) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (user == null || role == null) return;
        allRoles.remove(role);
    }

    @Override
    public Set<RoleModel> getApplicationRoleMappings(ApplicationModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();

        for (RoleModel role : allRoles) {
            RoleEntity roleEntity = ((RoleAdapter)role).getRoleEntity();
            if (app.getId().equals(roleEntity.getApplicationId())) {
                result.add(new RoleAdapter(realm, roleEntity, app));
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

    @Override
    public int compareTo(Object user) {
        if (this == user) return 0;
        return (getUsername().compareTo(((UserModel)user).getUsername()));
    }
}
