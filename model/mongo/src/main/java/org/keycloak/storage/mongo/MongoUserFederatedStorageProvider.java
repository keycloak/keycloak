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
package org.keycloak.storage.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.mongo.keycloak.entities.CredentialEntity;
import org.keycloak.models.mongo.keycloak.entities.FederatedIdentityEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.mongo.entity.FederatedUser;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MongoUserFederatedStorageProvider implements
        UserFederatedStorageProvider,
        UserCredentialStore {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;

    public MongoUserFederatedStorageProvider(KeycloakSession session, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.invocationContext = invocationContext;
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }


    protected FederatedUser addUserEntity(RealmModel realm, String id) {
        FederatedUser userEntity = new FederatedUser();
        userEntity.setId(id);
        userEntity.setStorageId(StorageId.providerId(id));
        userEntity.setRealmId(realm.getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return userEntity;
    }

    protected FederatedUser getUserById(String id) {
        return getMongoStore().loadEntity(FederatedUser.class, id, invocationContext);
    }

    protected FederatedUser findOrCreate(RealmModel realm, String id) {
        FederatedUser user = getUserById(id);
        if (user != null) return user;
        return addUserEntity(realm, id);
    }



    @Override
    public boolean removeStoredCredential(RealmModel realm, String userId, String id) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return false;
        CredentialEntity ce = getCredentialEntity(id, userEntity);
        if (ce != null) return getMongoStore().pullItemFromList(userEntity, "credentials", ce, invocationContext);
        return false;
    }

    private CredentialEntity getCredentialEntity(String id, FederatedUser userEntity) {
        CredentialEntity ce = null;
        if (userEntity.getCredentials() != null) {
            for (CredentialEntity credentialEntity : userEntity.getCredentials()) {
                if (credentialEntity.getId().equals(id)) {
                    ce = credentialEntity;
                    break;

                }
            }
        }
        return ce;
    }

    protected CredentialModel toModel(CredentialEntity entity) {
        CredentialModel model = new CredentialModel();
        model.setId(entity.getId());
        model.setHashIterations(entity.getHashIterations());
        model.setType(entity.getType());
        model.setValue(entity.getValue());
        model.setAlgorithm(entity.getAlgorithm());
        model.setSalt(entity.getSalt());
        model.setPeriod(entity.getPeriod());
        model.setCounter(entity.getCounter());
        model.setCreatedDate(entity.getCreatedDate());
        model.setDevice(entity.getDevice());
        model.setDigits(entity.getDigits());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        model.setConfig(config);
        if (entity.getConfig() != null) {
            config.putAll(entity.getConfig());
        }

        return model;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, String userId, String id) {
        FederatedUser userEntity = getUserById(id);
        if (userEntity != null && userEntity.getCredentials() != null) {
            for (CredentialEntity credentialEntity : userEntity.getCredentials()) {
                if (credentialEntity.getId().equals(id)) {
                    return toModel(credentialEntity);

                }
            }
        }
        return null;
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, String userId) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity != null && userEntity.getCredentials() != null) {
            List<CredentialModel> list = new LinkedList<>();
            for (CredentialEntity credentialEntity : userEntity.getCredentials()) {
                list.add(toModel(credentialEntity));
            }
            return list;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, String userId, String type) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity != null && userEntity.getCredentials() != null) {
            List<CredentialModel> list = new LinkedList<>();
            for (CredentialEntity credentialEntity : userEntity.getCredentials()) {
                if (type.equals(credentialEntity.getType())) list.add(toModel(credentialEntity));
            }
            return list;
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, String userId, String name, String type) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity != null && userEntity.getCredentials() != null) {
            for (CredentialEntity credentialEntity : userEntity.getCredentials()) {
                if (credentialEntity.getDevice().equals(name) && type.equals(credentialEntity.getType())) {
                    return toModel(credentialEntity);

                }
            }
        }
        return null;
    }

    @Override
    public List<String> getStoredUsers(RealmModel realm, int first, int max) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());

        DBObject query = queryBuilder.get();
        List<FederatedUser> users = getMongoStore().loadEntities(FederatedUser.class, query, null, first, max, invocationContext);
        List<String> ids = new LinkedList<>();
        for (FederatedUser user : users) ids.add(user.getId());
        return ids;
    }

    @Override
    public void preRemove(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        getMongoStore().removeEntities(FederatedUser.class, query, true, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        DBObject query = new QueryBuilder()
                .and("groupIds").is(group.getId())
                .get();

        DBObject pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(FederatedUser.class, query, pull, invocationContext);

    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        DBObject query = new QueryBuilder()
                .and("roleIds").is(role.getId())
                .get();

        DBObject pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(FederatedUser.class, query, pull, invocationContext);

    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {

    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {

    }

    @Override
    public void preRemove(RealmModel realm, UserModel user) {
        getMongoStore().removeEntity(FederatedUser.class, user.getId(), invocationContext);

    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel model) {
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) return;
        DBObject query = new QueryBuilder()
                .and("storageId").is(model.getId())
                .get();
        getMongoStore().removeEntities(FederatedUser.class, query, true, invocationContext);

    }

    @Override
    public void close() {

    }

    @Override
    public void setSingleAttribute(RealmModel realm, String userId, String name, String value) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        if (userEntity.getAttributes() == null) {
            userEntity.setAttributes(new HashMap<>());
        }

        List<String> attrValues = new LinkedList<>();
        attrValues.add(value);
        userEntity.getAttributes().put(name, attrValues);
        getMongoStore().updateEntity(userEntity, invocationContext);
    }

    @Override
    public void setAttribute(RealmModel realm, String userId, String name, List<String> values) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        if (userEntity.getAttributes() == null) {
            userEntity.setAttributes(new HashMap<>());
        }

        userEntity.getAttributes().put(name, values);
        getMongoStore().updateEntity(userEntity, invocationContext);

    }

    @Override
    public void removeAttribute(RealmModel realm, String userId, String name) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getAttributes() == null) return;

        userEntity.getAttributes().remove(name);
        getMongoStore().updateEntity(userEntity, invocationContext);
    }

    @Override
    public MultivaluedHashMap<String, String> getAttributes(RealmModel realm, String userId) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getAttributes() == null) return new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        result.putAll(userEntity.getAttributes());
        return result;
    }

    @Override
    public List<String> getUsersByUserAttribute(RealmModel realm, String name, String value) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());
        queryBuilder.and("attributes." + name).is(value);

        List<FederatedUser> users = getMongoStore().loadEntities(FederatedUser.class, queryBuilder.get(), invocationContext);
        List<String> ids = new LinkedList<>();
        for (FederatedUser user : users) ids.add(user.getId());
        return ids;
    }

    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("federatedIdentities.identityProvider").is(socialLink.getIdentityProvider())
                .and("federatedIdentities.userId").is(socialLink.getUserId())
                .and("realmId").is(realm.getId())
                .get();
        FederatedUser userEntity = getMongoStore().loadSingleEntity(FederatedUser.class, query, invocationContext);
        return userEntity != null ? userEntity.getId() : null;
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel socialLink) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        FederatedIdentityEntity federatedIdentityEntity = new FederatedIdentityEntity();
        federatedIdentityEntity.setIdentityProvider(socialLink.getIdentityProvider());
        federatedIdentityEntity.setUserId(socialLink.getUserId());
        federatedIdentityEntity.setUserName(socialLink.getUserName().toLowerCase());
        federatedIdentityEntity.setToken(socialLink.getToken());

        getMongoStore().pushItemToList(userEntity, "federatedIdentities", federatedIdentityEntity, true, invocationContext);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, String userId, String socialProvider) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return false;

        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, socialProvider);
        if (federatedIdentityEntity == null) {
            return false;
        }
        return getMongoStore().pullItemFromList(userEntity, "federatedIdentities", federatedIdentityEntity, invocationContext);    }

    private FederatedIdentityEntity findFederatedIdentityLink(FederatedUser userEntity, String identityProvider) {
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();
        if (linkEntities == null) {
            return null;
        }

        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            if (federatedIdentityEntity.getIdentityProvider().equals(identityProvider)) {
                return federatedIdentityEntity;
            }
        }
        return null;
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, String userId, FederatedIdentityModel federatedIdentityModel) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return;
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, federatedIdentityModel.getIdentityProvider());
        if (federatedIdentityEntity == null) return;
        //pushItemToList updates the whole federatedIdentities array in Mongo so we just need to remove this object from the Java
        //List and pushItemToList will handle the DB update.
        userEntity.getFederatedIdentities().remove(federatedIdentityEntity);
        federatedIdentityEntity.setToken(federatedIdentityModel.getToken());
        getMongoStore().pushItemToList(userEntity, "federatedIdentities", federatedIdentityEntity, true, invocationContext);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(String userId, RealmModel realm) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return Collections.EMPTY_SET;
        List<FederatedIdentityEntity> linkEntities = userEntity.getFederatedIdentities();

        if (linkEntities == null) {
            return Collections.EMPTY_SET;
        }

        Set<FederatedIdentityModel> result = new HashSet<FederatedIdentityModel>();
        for (FederatedIdentityEntity federatedIdentityEntity : linkEntities) {
            FederatedIdentityModel model = new FederatedIdentityModel(federatedIdentityEntity.getIdentityProvider(),
                    federatedIdentityEntity.getUserId(), federatedIdentityEntity.getUserName(), federatedIdentityEntity.getToken());
            result.add(model);
        }
        return result;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(String userId, String socialProvider, RealmModel realm) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return null;
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, socialProvider);

        return federatedIdentityEntity != null ? new FederatedIdentityModel(federatedIdentityEntity.getIdentityProvider(), federatedIdentityEntity.getUserId(),
                federatedIdentityEntity.getUserName(), federatedIdentityEntity.getToken()) : null;
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        session.userLocalStorage().addConsent(realm, userId, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientInternalId) {
        return session.userLocalStorage().getConsentByClient(realm, userId, clientInternalId);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        return session.userLocalStorage().getConsents(realm, userId);
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        session.userLocalStorage().updateConsent(realm, userId, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientInternalId) {
        return session.userLocalStorage().revokeConsentForClient(realm, userId, clientInternalId);
    }

    @Override
    public void updateCredential(RealmModel realm, String userId, CredentialModel cred) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null) return;
        CredentialEntity entity = getCredentialEntity(cred.getId(), userEntity);
        if (entity == null) return;
        toEntity(cred, entity);
        userEntity.getCredentials().remove(entity);

        getMongoStore().pushItemToList(userEntity, "credentials", entity, true, invocationContext);
    }

    private void toEntity(CredentialModel cred, CredentialEntity entity) {
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        entity.setCreatedDate(cred.getCreatedDate());
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());

        if (cred.getConfig() == null) entity.setConfig(null);
        else {
            MultivaluedHashMap<String, String> newConfig = new MultivaluedHashMap<>();
            newConfig.putAll(cred.getConfig());
            entity.setConfig(newConfig);
        }
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, String userId, CredentialModel cred) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        CredentialEntity entity = new CredentialEntity();
        entity.setId(KeycloakModelUtils.generateId());
        toEntity(cred, entity);
        getMongoStore().pushItemToList(userEntity, "credentials", entity, true, invocationContext);
        cred.setId(entity.getId());
        return cred;
    }

    @Override
    public Set<GroupModel> getGroups(RealmModel realm, String userId) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getGroupIds() == null || userEntity.getGroupIds().isEmpty()) return Collections.EMPTY_SET;
        Set<GroupModel> groups = new HashSet<>();
        for (String groupId : userEntity.getGroupIds()) {
            GroupModel group = session.realms().getGroupById(groupId, realm);
            if (group != null) groups.add(group);
        }

        return groups;
    }

    @Override
    public void joinGroup(RealmModel realm, String userId, GroupModel group) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        getMongoStore().pushItemToList(userEntity, "groupIds", group.getId(), true, invocationContext);


    }

    @Override
    public void leaveGroup(RealmModel realm, String userId, GroupModel group) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || group == null) return;
        getMongoStore().pullItemFromList(userEntity, "groupIds", group.getId(), invocationContext);


    }

    @Override
    public List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());
        queryBuilder.and("groupIds").is(group.getId());

        List<FederatedUser> users = getMongoStore().loadEntities(FederatedUser.class, queryBuilder.get(), null, firstResult, max, invocationContext);
        List<String> ids = new LinkedList<>();
        for (FederatedUser user : users) ids.add(user.getId());
        return ids;
    }

    @Override
    public Set<String> getRequiredActions(RealmModel realm, String userId) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getRequiredActions() == null || userEntity.getRequiredActions().isEmpty()) return Collections.EMPTY_SET;
        Set<String> set = new HashSet<>();
        set.addAll(userEntity.getRequiredActions());
        return set;
    }

    @Override
    public void addRequiredAction(RealmModel realm, String userId, String action) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        getMongoStore().pushItemToList(userEntity, "requiredActions", action, true, invocationContext);

    }

    @Override
    public void removeRequiredAction(RealmModel realm, String userId, String action) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getRequiredActions() == null || userEntity.getRequiredActions().isEmpty()) return;
        getMongoStore().pullItemFromList(userEntity, "requiredActions", action, invocationContext);

    }

    @Override
    public void grantRole(RealmModel realm, String userId, RoleModel role) {
        FederatedUser userEntity = findOrCreate(realm, userId);
        getMongoStore().pushItemToList(userEntity, "roleIds", role.getId(), true, invocationContext);

    }

    @Override
    public Set<RoleModel> getRoleMappings(RealmModel realm, String userId) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getRoleIds() == null || userEntity.getRoleIds().isEmpty()) return Collections.EMPTY_SET;
        Set<RoleModel> roles = new HashSet<>();
        for (String roleId : userEntity.getRoleIds()) {
            RoleModel role = realm.getRoleById(roleId);
            if (role != null) roles.add(role);
        }
        return roles;
    }

    @Override
    public void deleteRoleMapping(RealmModel realm, String userId, RoleModel role) {
        FederatedUser userEntity = getUserById(userId);
        if (userEntity == null || userEntity.getRoleIds() == null || userEntity.getRoleIds().isEmpty()) return;
        getMongoStore().pullItemFromList(userEntity, "roleIds", role.getId(), invocationContext);

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        updateCredential(realm, user.getId(), cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        return createCredential(realm, user.getId(), cred);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        return removeStoredCredential(realm, user.getId(), id);
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return getStoredCredentialById(realm, user.getId(), id);
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        return getStoredCredentials(realm, user.getId());
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        return getStoredCredentialsByType(realm, user.getId(), type);
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return getStoredCredentialByNameAndType(realm, user.getId(), name, type);
    }

    @Override
    public int getStoredUsersCount(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        return getMongoStore().countEntities(FederatedUser.class, query, invocationContext);
    }
}
