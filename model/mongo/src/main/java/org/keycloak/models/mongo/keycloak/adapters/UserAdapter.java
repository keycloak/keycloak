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

package org.keycloak.models.mongo.keycloak.adapters;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.hash.PasswordHashManager;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.OTPPolicy;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.entities.CredentialEntity;
import org.keycloak.models.entities.UserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.mongo.utils.MongoModelUtils;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.common.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around UserData object, which will persist wrapped object after each set operation (compatibility with picketlink based idm)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserAdapter extends AbstractMongoAdapter<MongoUserEntity> implements UserModel {
    
    private final MongoUserEntity user;
    private final RealmModel realm;
    private final KeycloakSession session;

    public UserAdapter(KeycloakSession session, RealmModel realm, MongoUserEntity userEntity, MongoStoreInvocationContext invContext) {
        super(invContext);
        this.user = userEntity;
        this.realm = realm;
        this.session = session;
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
        username = KeycloakModelUtils.toLowerCaseSafe(username);

        user.setUsername(username);
        updateUser();
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
        updateUser();
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
        updateUser();
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
        updateUser();
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        email = KeycloakModelUtils.toLowerCaseSafe(email);

        user.setEmail(email);
        updateUser();
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
        updateUser();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        List<String> attrValues = new ArrayList<>();
        attrValues.add(value);
        user.getAttributes().put(name, attrValues);
        updateUser();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<String, List<String>>());
        }

        user.getAttributes().put(name, values);
        updateUser();
    }

    @Override
    public void removeAttribute(String name) {
        if (user.getAttributes() == null) return;

        user.getAttributes().remove(name);
        updateUser();
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
        return user.getAttributes()==null ? Collections.<String, List<String>>emptyMap() : Collections.unmodifiableMap((Map) user.getAttributes());
    }

    public MongoUserEntity getUser() {
        return user;
    }


    @Override
    public Set<String> getRequiredActions() {
        Set<String> result = new HashSet<String>();
        if (user.getRequiredActions() != null) {
            result.addAll(user.getRequiredActions());
        }
        return result;
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        String actionName = action.name();
        addRequiredAction(actionName);
    }

    @Override
    public void addRequiredAction(String actionName) {
        getMongoStore().pushItemToList(user, "requiredActions", actionName, true, invocationContext);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        String actionName = action.name();
        removeRequiredAction(actionName);
    }

    @Override
    public void removeRequiredAction(String actionName) {
        getMongoStore().pullItemFromList(user, "requiredActions", actionName, invocationContext);
    }

    @Override
    public boolean isOtpEnabled() {
        return user.isTotp();
    }

    @Override
    public void setOtpEnabled(boolean totp) {
        user.setTotp(totp);
        updateUser();
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {

        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            updatePasswordCredential(cred);
        } else if (UserCredentialModel.isOtp(cred.getType())){
            updateOtpCredential(cred);
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
        getMongoStore().updateEntity(user, invocationContext);
    }

    private void updateOtpCredential(UserCredentialModel cred) {
        CredentialEntity credentialEntity = getCredentialEntity(user, cred.getType());

        if (credentialEntity == null) {
            credentialEntity = setCredentials(user, cred);
            credentialEntity.setValue(cred.getValue());
            OTPPolicy otpPolicy = realm.getOTPPolicy();
            credentialEntity.setAlgorithm(otpPolicy.getAlgorithm());
            credentialEntity.setDigits(otpPolicy.getDigits());
            credentialEntity.setCounter(otpPolicy.getInitialCounter());
            credentialEntity.setPeriod(otpPolicy.getPeriod());
            user.getCredentials().add(credentialEntity);
        } else {
            credentialEntity.setValue(cred.getValue());
            OTPPolicy policy = realm.getOTPPolicy();
            credentialEntity.setDigits(policy.getDigits());
            credentialEntity.setCounter(policy.getInitialCounter());
            credentialEntity.setAlgorithm(policy.getAlgorithm());
            credentialEntity.setPeriod(policy.getPeriod());
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
    
    private CredentialEntity setCredentials(MongoUserEntity user, UserCredentialModel cred) {
        CredentialEntity credentialEntity = new CredentialEntity();
        credentialEntity.setType(cred.getType());
        credentialEntity.setDevice(cred.getDevice());
        return credentialEntity;
    }

    private void setValue(CredentialEntity credentialEntity, UserCredentialModel cred) {
        UserCredentialValueModel encoded = PasswordHashManager.encode(session, realm, cred.getValue());
        credentialEntity.setCreatedDate(Time.toMillis(Time.currentTime()));
        credentialEntity.setAlgorithm(encoded.getAlgorithm());
        credentialEntity.setValue(encoded.getValue());
        credentialEntity.setSalt(encoded.getSalt());
        credentialEntity.setHashIterations(encoded.getHashIterations());
    }

    private CredentialEntity getCredentialEntity(MongoUserEntity userEntity, String credType) {
        for (CredentialEntity entity : userEntity.getCredentials()) {
            if (entity.getType().equals(credType)) {
                return entity;
            }
        }

        return null;
    }

    private List<CredentialEntity> getCredentialEntities(MongoUserEntity userEntity, String credType) {
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
        List<CredentialEntity> credentials = user.getCredentials();
        List<UserCredentialValueModel> result = new ArrayList<UserCredentialValueModel>();
        for (CredentialEntity credEntity : credentials) {
            UserCredentialValueModel credModel = new UserCredentialValueModel();
            credModel.setType(credEntity.getType());
            credModel.setDevice(credEntity.getDevice());
            credModel.setCreatedDate(credEntity.getCreatedDate());
            credModel.setValue(credEntity.getValue());
            credModel.setSalt(credEntity.getSalt());
            credModel.setHashIterations(credEntity.getHashIterations());
            credModel.setAlgorithm(credEntity.getAlgorithm());

            if (UserCredentialModel.isOtp(credEntity.getType())) {
                credModel.setCounter(credEntity.getCounter());
                if (credEntity.getAlgorithm() == null) {
                    // for migration where these values would be null
                    credModel.setAlgorithm(realm.getOTPPolicy().getAlgorithm());
                } else {
                    credModel.setAlgorithm(credEntity.getAlgorithm());
                }
                if (credEntity.getDigits() == 0) {
                    // for migration where these values would be 0
                    credModel.setDigits(realm.getOTPPolicy().getDigits());
                } else {
                    credModel.setDigits(credEntity.getDigits());
                }

                if (credEntity.getPeriod() == 0) {
                    // for migration where these values would be 0
                    credModel.setPeriod(realm.getOTPPolicy().getPeriod());
                } else {
                    credModel.setPeriod(credEntity.getPeriod());
                }
            }

            result.add(credModel);
        }

        return result;
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel credModel) {
        CredentialEntity credentialEntity = getCredentialEntity(user, credModel.getType());

        if (credentialEntity == null) {
            credentialEntity = new CredentialEntity();
            credentialEntity.setType(credModel.getType());
            credModel.setCreatedDate(credModel.getCreatedDate());
            user.getCredentials().add(credentialEntity);
        }

        credentialEntity.setValue(credModel.getValue());
        credentialEntity.setSalt(credModel.getSalt());
        credentialEntity.setDevice(credModel.getDevice());
        credentialEntity.setHashIterations(credModel.getHashIterations());
        credentialEntity.setCounter(credModel.getCounter());
        credentialEntity.setAlgorithm(credModel.getAlgorithm());
        credentialEntity.setDigits(credModel.getDigits());
        credentialEntity.setPeriod(credModel.getPeriod());


        getMongoStore().updateEntity(user, invocationContext);
    }

    protected void updateUser() {
        super.updateMongoEntity();
    }

    @Override
    public MongoUserEntity getMongoEntity() {
        return user;
    }

    @Override
    public Set<GroupModel> getGroups() {
        if (user.getGroupIds() == null || user.getGroupIds().size() == 0) return Collections.EMPTY_SET;
        Set<GroupModel> groups = new HashSet<>();
        for (String id : user.getGroupIds()) {
            groups.add(realm.getGroupById(id));
        }
        return groups;
    }

    @Override
    public void joinGroup(GroupModel group) {
        getMongoStore().pushItemToList(getUser(), "groupIds", group.getId(), true, invocationContext);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        if (user == null || group == null) return;

        getMongoStore().pullItemFromList(getUser(), "groupIds", group.getId(), invocationContext);

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        if (user.getGroupIds().contains(group.getId())) return true;
        Set<GroupModel> groups = getGroups();
        return KeycloakModelUtils.isMember(groups, group);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        Set<RoleModel> roles = getRoleMappings();
        return KeycloakModelUtils.hasRole(roles, role);
    }

    @Override
    public void grantRole(RoleModel role) {
        getMongoStore().pushItemToList(getUser(), "roleIds", role.getId(), true, invocationContext);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        List<RoleModel> roles = MongoModelUtils.getAllRolesOfUser(realm, this);
        return new HashSet<RoleModel>(roles);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> allRoles = getRoleMappings();

        // Filter to retrieve just realm roles
        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allRoles) {
            if (role.getContainer() instanceof RealmModel) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (user == null || role == null) return;

        getMongoStore().pullItemFromList(getUser(), "roleIds", role.getId(), invocationContext);
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> result = new HashSet<RoleModel>();
        List<RoleModel> roles = MongoModelUtils.getAllRolesOfUser(realm, this);

        for (RoleModel role : roles) {
            if (app.equals(role.getContainer())) {
                result.add(role);
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
        updateUser();
    }

    @Override
    public String getServiceAccountClientLink() {
        return user.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        user.setServiceAccountClientLink(clientInternalId);
        updateUser();
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
