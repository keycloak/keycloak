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

import org.keycloak.connections.file.InMemoryModel;
import org.keycloak.models.ClientModel;

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.getSalt;

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.entities.FederatedIdentityEntity;
import org.keycloak.models.entities.RoleEntity;
import org.keycloak.models.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.Pbkdf2PasswordEncoder;
import org.keycloak.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.keycloak.models.utils.Pbkdf2PasswordEncoder.getSalt;

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
    public Long getCreatedTimestamp() {
        return user.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        user.setCreatedTimestamp(timestamp);
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
    public void setSingleAttribute(String name, String value) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        List<String> attrValues = new ArrayList<>();
        attrValues.add(value);
        user.getAttributes().put(name, attrValues);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        user.getAttributes().put(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        if (user.getAttributes() == null) return;

        user.getAttributes().remove(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (user.getAttributes()==null) return null;

        List<String> attrValues = user.getAttributes().get(name);
        return (attrValues==null || attrValues.isEmpty()) ? null : attrValues.get(0);
    }

    @Override
    public List<String> getAttribute(String name) {
        if (user.getAttributes()==null) return Collections.<String>emptyList();
        List<String> attrValues = user.getAttributes().get(name);
        return (attrValues == null) ? Collections.<String>emptyList() : Collections.unmodifiableList(attrValues);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return user.getAttributes()==null ? Collections.<String, List<String>>emptyMap() : Collections.unmodifiableMap((Map)user.getAttributes());
    }

    @Override
    public Set<String> getRequiredActions() {
        List<String> requiredActions = user.getRequiredActions();
        if (requiredActions == null) requiredActions = new ArrayList<String>();
        return new HashSet(requiredActions);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        String actionName = action.name();
        addRequiredAction(actionName);
    }

    @Override
    public void addRequiredAction(String actionName) {
        List<String> requiredActions = user.getRequiredActions();
        if (requiredActions == null) requiredActions = new ArrayList<>();
        if (!requiredActions.contains(actionName)) {
            requiredActions.add(actionName);
        }
        user.setRequiredActions(requiredActions);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        String actionName = action.name();
        removeRequiredAction(actionName);
    }

    @Override
    public void removeRequiredAction(String actionName) {
        List<String> requiredActions = user.getRequiredActions();
        if (requiredActions == null) return;
        requiredActions.remove(actionName);
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

        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            updatePasswordCredential(cred);
        } else {
            CredentialEntity credentialEntity = getCredentialEntity(user, cred.getType());

            if (credentialEntity == null) {
                credentialEntity = setCredentials(user, cred);
                credentialEntity.setValue(cred.getValue());
                user.getCredentials().add(credentialEntity);
            } else {
                credentialEntity.setValue(cred.getValue());
            }
        }
    }

    private void updatePasswordCredential(UserCredentialModel cred) {
        CredentialEntity credentialEntity = getCredentialEntity(user, cred.getType());

        if (credentialEntity == null) {
            credentialEntity = setCredentials(user, cred);
            setValue(credentialEntity, cred);
            user.getCredentials().add(credentialEntity);
        } else {

            int expiredPasswordsPolicyValue = -1;
            PasswordPolicy policy = realm.getPasswordPolicy();
            if(policy != null) {
                expiredPasswordsPolicyValue = policy.getExpiredPasswords();
            }
            
            if (expiredPasswordsPolicyValue != -1) {
                user.getCredentials().remove(credentialEntity);
                credentialEntity.setType(UserCredentialModel.PASSWORD_HISTORY);
                user.getCredentials().add(credentialEntity);

                List<CredentialEntity> credentialEntities = getCredentialEntities(user, UserCredentialModel.PASSWORD_HISTORY);
                if (credentialEntities.size() > expiredPasswordsPolicyValue - 1) {
                    user.getCredentials().removeAll(credentialEntities.subList(expiredPasswordsPolicyValue - 1, credentialEntities.size()));
                }

                credentialEntity = setCredentials(user, cred);
                setValue(credentialEntity, cred);
                user.getCredentials().add(credentialEntity);
            } else {
                List<CredentialEntity> credentialEntities = getCredentialEntities(user, UserCredentialModel.PASSWORD_HISTORY);
                if (credentialEntities != null && credentialEntities.size() > 0) {
                    user.getCredentials().removeAll(credentialEntities);
                }
                setValue(credentialEntity, cred);
            }
        }
    }
    
    private CredentialEntity setCredentials(UserEntity user, UserCredentialModel cred) {
        CredentialEntity credentialEntity = new CredentialEntity();
        credentialEntity.setType(cred.getType());
        credentialEntity.setDevice(cred.getDevice());
        return credentialEntity;
    }

    private void setValue(CredentialEntity credentialEntity, UserCredentialModel cred) {
        byte[] salt = getSalt();
        int hashIterations = 1;
        PasswordPolicy policy = realm.getPasswordPolicy();
        if (policy != null) {
            hashIterations = policy.getHashIterations();
            if (hashIterations == -1)
                hashIterations = 1;
        }
        credentialEntity.setCreatedDate(Time.toMillis(Time.currentTime()));
        credentialEntity.setValue(new Pbkdf2PasswordEncoder(salt).encode(cred.getValue(), hashIterations));
        credentialEntity.setSalt(salt);
        credentialEntity.setHashIterations(hashIterations);
    }

    private CredentialEntity getCredentialEntity(UserEntity userEntity, String credType) {
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }

        return null;
    }

    private List<CredentialEntity> getCredentialEntities(UserEntity userEntity, String credType) {
        List<CredentialEntity> credentialEntities = new ArrayList<CredentialEntity>();
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                credentialEntities.add(entity);
            }
        }
        
        // Avoiding direct use of credSecond.getCreatedDate() - credFirst.getCreatedDate() to prevent Integer Overflow
        // Orders from most recent to least recent
        Collections.sort(credentialEntities, new Comparator<CredentialEntity>() {
            public int compare(CredentialEntity credFirst, CredentialEntity credSecond) {
                if (credFirst.getCreatedDate() > credSecond.getCreatedDate()) {
                    return -1;
                } else if (credFirst.getCreatedDate() < credSecond.getCreatedDate()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return credentialEntities;
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        List<CredentialEntity> credentials = new ArrayList<CredentialEntity>(user.getCredentials());
        List<UserCredentialValueModel> result = new ArrayList<UserCredentialValueModel>();

        for (CredentialEntity credEntity : credentials) {
            UserCredentialValueModel credModel = new UserCredentialValueModel();
            credModel.setType(credEntity.getType());
            credModel.setDevice(credEntity.getDevice());
            credModel.setCreatedDate(credEntity.getCreatedDate());
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
            credModel.setCreatedDate(credModel.getCreatedDate());
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
        return KeycloakModelUtils.hasRole(roles, role);
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
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();

        for (RoleModel role : allRoles) {
            RoleEntity roleEntity = ((RoleAdapter)role).getRoleEntity();
            if (app.getId().equals(roleEntity.getClientId())) {
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
    public void addConsent(UserConsentModel consent) {
        // TODO
    }

    @Override
    public UserConsentModel getConsentByClient(String clientId) {
        // TODO
        return null;
    }

    @Override
    public List<UserConsentModel> getConsents() {
        // TODO
        return null;
    }

    @Override
    public void updateConsent(UserConsentModel consent) {
        // TODO
    }

    @Override
    public boolean revokeConsentForClient(String clientId) {
        // TODO
        return false;
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
