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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;

import org.keycloak.component.ComponentModel;
import org.keycloak.connections.mongo.api.MongoStore;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.entities.FederatedIdentityEntity;
import org.keycloak.models.entities.UserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserConsentEntity;
import org.keycloak.models.mongo.keycloak.entities.MongoUserEntity;
import org.keycloak.models.utils.CredentialValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MongoUserProvider implements UserProvider {

    private final MongoStoreInvocationContext invocationContext;
    private final KeycloakSession session;

    public MongoUserProvider(KeycloakSession session, MongoStoreInvocationContext invocationContext) {
        this.session = session;
        this.invocationContext = invocationContext;
    }

    @Override
    public void close() {
    }

    @Override
    public UserAdapter getUserById(String id, RealmModel realm) {
        MongoUserEntity user = getMongoStore().loadEntity(MongoUserEntity.class, id, invocationContext);

        // Check that it's user from this realm
        if (user == null || !realm.getId().equals(user.getRealmId())) {
            return null;
        } else {
            return new UserAdapter(session, realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("username").is(username.toLowerCase())
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(session, realm, user, invocationContext);
        }
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("email").is(email.toLowerCase())
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity user = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);

        if (user == null) {
            return null;
        } else {
            return new UserAdapter(session, realm, user, invocationContext);
        }
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());
        queryBuilder.and("groupIds").is(group.getId());
        DBObject sort = new BasicDBObject("username", 1);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, queryBuilder.get(), sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    protected MongoStore getMongoStore() {
        return invocationContext.getMongoStore();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return getGroupMembers(realm, group, -1, -1);
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel socialLink, RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("federatedIdentities.identityProvider").is(socialLink.getIdentityProvider())
                .and("federatedIdentities.userId").is(socialLink.getUserId())
                .and("realmId").is(realm.getId())
                .get();
        MongoUserEntity userEntity = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);
        return userEntity == null ? null : new UserAdapter(session, realm, userEntity, invocationContext);
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        DBObject query = new QueryBuilder()
                .and("serviceAccountClientLink").is(client.getId())
                .and("realmId").is(client.getRealm().getId())
                .get();
        MongoUserEntity userEntity = getMongoStore().loadSingleEntity(MongoUserEntity.class, query, invocationContext);
        return userEntity == null ? null : new UserAdapter(session, client.getRealm(), userEntity, invocationContext);
    }

    protected List<UserModel> convertUserEntities(RealmModel realm, List<MongoUserEntity> userEntities) {
        List<UserModel> userModels = new ArrayList<UserModel>();
        for (MongoUserEntity user : userEntities) {
            userModels.add(new UserAdapter(session, realm, user, invocationContext));
        }
        return userModels;
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, false);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
        return getUsers(realm, firstResult, maxResults, false);
    }



    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, -1, -1, includeServiceAccounts);
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        return getMongoStore().countEntities(MongoUserEntity.class, query, invocationContext);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());

        if (!includeServiceAccounts) {
            queryBuilder = queryBuilder.and("serviceAccountClientLink").is(null);
        }

        DBObject query = queryBuilder.get();
        DBObject sort = new BasicDBObject("username", 1);
        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, query, sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel>
    searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        search = search.trim();
        Pattern caseInsensitivePattern = Pattern.compile("(?i:" + search + ")");

        QueryBuilder nameBuilder;
        int spaceInd = search.lastIndexOf(" ");

        // Case when we have search string like "ohn Bow". Then firstName must end with "ohn" AND lastName must start with "bow" (everything case-insensitive)
        if (spaceInd != -1) {
            String firstName = search.substring(0, spaceInd);
            String lastName = search.substring(spaceInd + 1);
            Pattern firstNamePattern = Pattern.compile("(?i:" + firstName + "$)");
            Pattern lastNamePattern = Pattern.compile("(?i:^" + lastName + ")");
            nameBuilder = new QueryBuilder().and(
                    new QueryBuilder().put("firstName").regex(firstNamePattern).get(),
                    new QueryBuilder().put("lastName").regex(lastNamePattern).get()
            );
        } else {
            // Case when we have search without spaces like "foo". The firstName OR lastName could be "foo" (everything case-insensitive)
            nameBuilder = new QueryBuilder().or(
                    new QueryBuilder().put("firstName").regex(caseInsensitivePattern).get(),
                    new QueryBuilder().put("lastName").regex(caseInsensitivePattern).get()
            );
        }

        QueryBuilder builder = new QueryBuilder().and(
                new QueryBuilder().and("realmId").is(realm.getId()).get(),
                new QueryBuilder().and("serviceAccountClientLink").is(null).get(),
                new QueryBuilder().or(
                        new QueryBuilder().put("username").regex(caseInsensitivePattern).get(),
                        new QueryBuilder().put("email").regex(caseInsensitivePattern).get(),
                        nameBuilder.get()

                ).get()
        );

        DBObject sort = new BasicDBObject("username", 1);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, builder.get(), sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(UserModel.USERNAME)) {
                queryBuilder.and(UserModel.USERNAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                queryBuilder.and(UserModel.FIRST_NAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                queryBuilder.and(UserModel.LAST_NAME).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));

            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                queryBuilder.and(UserModel.EMAIL).regex(Pattern.compile(".*" + entry.getValue() + ".*", Pattern.CASE_INSENSITIVE));
            }
        }

        DBObject sort = new BasicDBObject("username", 1);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, queryBuilder.get(), sort, firstResult, maxResults, invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        QueryBuilder queryBuilder = new QueryBuilder()
                .and("realmId").is(realm.getId());
        queryBuilder.and("attributes." + attrName).is(attrValue);

        List<MongoUserEntity> users = getMongoStore().loadEntities(MongoUserEntity.class, queryBuilder.get(), invocationContext);
        return convertUserEntities(realm, users);
    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel userModel, RealmModel realm) {
        UserAdapter user = getUserById(userModel.getId(), realm);
        MongoUserEntity userEntity = user.getUser();
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
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        UserAdapter mongoUser = getUserById(user.getId(), realm);
        MongoUserEntity userEntity = mongoUser.getUser();
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, socialProvider);

        return federatedIdentityEntity != null ? new FederatedIdentityModel(federatedIdentityEntity.getIdentityProvider(), federatedIdentityEntity.getUserId(),
                federatedIdentityEntity.getUserName(), federatedIdentityEntity.getToken()) : null;
    }

    @Override
    public UserAdapter addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        UserAdapter userModel = addUserEntity(realm, id, username.toLowerCase());

        if (addDefaultRoles) {
            for (String r : realm.getDefaultRoles()) {
                userModel.grantRole(realm.getRole(r));
            }

            for (ClientModel application : realm.getClients()) {
                for (String r : application.getDefaultRoles()) {
                    userModel.grantRole(application.getRole(r));
                }
            }
            for (GroupModel g : realm.getDefaultGroups()) {
                userModel.joinGroup(g);
            }
        }

        if (addDefaultRequiredActions) {
            for (RequiredActionProviderModel r : realm.getRequiredActionProviders()) {
                if (r.isEnabled() && r.isDefaultAction()) {
                    userModel.addRequiredAction(r.getAlias());
                }
            }
        }


        return userModel;
    }

    protected UserAdapter addUserEntity(RealmModel realm, String id, String username) {
        MongoUserEntity userEntity = new MongoUserEntity();
        userEntity.setId(id);
        userEntity.setUsername(username);
        userEntity.setCreatedTimestamp(System.currentTimeMillis());
        // Compatibility with JPA model, which has user disabled by default
        // userEntity.setEnabled(true);
        userEntity.setRealmId(realm.getId());

        getMongoStore().insertEntity(userEntity, invocationContext);
        return new UserAdapter(session, realm, userEntity, invocationContext);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        return getMongoStore().removeEntity(MongoUserEntity.class, user.getId(), invocationContext);
    }


    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel identity) {
        UserAdapter mongoUser = getUserById(user.getId(), realm);
        MongoUserEntity userEntity = mongoUser.getUser();
        FederatedIdentityEntity federatedIdentityEntity = new FederatedIdentityEntity();
        federatedIdentityEntity.setIdentityProvider(identity.getIdentityProvider());
        federatedIdentityEntity.setUserId(identity.getUserId());
        federatedIdentityEntity.setUserName(identity.getUserName().toLowerCase());
        federatedIdentityEntity.setToken(identity.getToken());

        getMongoStore().pushItemToList(userEntity, "federatedIdentities", federatedIdentityEntity, true, invocationContext);
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        UserAdapter mongoUser = getUserById(federatedUser.getId(), realm);
        MongoUserEntity userEntity = mongoUser.getUser();
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, federatedIdentityModel.getIdentityProvider());

        //pushItemToList updates the whole federatedIdentities array in Mongo so we just need to remove this object from the Java
        //List and pushItemToList will handle the DB update.
        userEntity.getFederatedIdentities().remove(federatedIdentityEntity);
        federatedIdentityEntity.setToken(federatedIdentityModel.getToken());
        getMongoStore().pushItemToList(userEntity, "federatedIdentities", federatedIdentityEntity, true, invocationContext);
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel userModel, String socialProvider) {
        UserAdapter user = getUserById(userModel.getId(), realm);
        MongoUserEntity userEntity = user.getUser();
        FederatedIdentityEntity federatedIdentityEntity = findFederatedIdentityLink(userEntity, socialProvider);
        if (federatedIdentityEntity == null) {
            return false;
        }
        return getMongoStore().pullItemFromList(userEntity, "federatedIdentities", federatedIdentityEntity, invocationContext);
    }

    private FederatedIdentityEntity findFederatedIdentityLink(MongoUserEntity userEntity, String identityProvider) {
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
    public UserModel addUser(RealmModel realm, String username) {
        return this.addUser(realm, null, username, true, true);
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();

        DBObject update = new QueryBuilder()
                .and("$push").is(new BasicDBObject("roleIds", role.getId()))
                .get();

        int count = getMongoStore().updateEntities(MongoUserEntity.class, query, update, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm) {
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .get();
        getMongoStore().removeEntities(MongoUserEntity.class, query, true, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        // Remove all users linked with federationProvider and their consents
        DBObject query = new QueryBuilder()
                .and("realmId").is(realm.getId())
                .and("federationLink").is(link.getId())
                .get();
        getMongoStore().removeEntities(MongoUserEntity.class, query, true, invocationContext);

    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        // Remove all role mappings and consents mapped to all roles of this client
        for (RoleModel role : client.getRoles()) {
            preRemove(realm, role);
        }

        // Finally remove all consents of this client
        DBObject query = new QueryBuilder()
                .and("clientId").is(client.getId())
                .get();
        getMongoStore().removeEntities(MongoUserConsentEntity.class, query, false, invocationContext);
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        // Remove this protocol mapper from all consents, which has it
        DBObject query = new QueryBuilder()
                .and("grantedProtocolMappers").is(protocolMapper.getId())
                .get();
        DBObject pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(MongoUserConsentEntity.class, query, pull, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        // Remove this role from all users, which has it
        DBObject query = new QueryBuilder()
                .and("groupIds").is(group.getId())
                .get();

        DBObject pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(MongoUserEntity.class, query, pull, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        // Remove this role from all users, which has it
        DBObject query = new QueryBuilder()
                .and("roleIds").is(role.getId())
                .get();

        DBObject pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(MongoUserEntity.class, query, pull, invocationContext);

        // Remove this role from all consents, which has it
        query = new QueryBuilder()
                .and("grantedRoles").is(role.getId())
                .get();
        pull = new BasicDBObject("$pull", query);
        getMongoStore().updateEntities(MongoUserConsentEntity.class, query, pull, invocationContext);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, List<UserCredentialModel> input) {
        return CredentialValidation.validCredentials(session, realm, user, input);
    }

    @Override
    public boolean validCredentials(KeycloakSession session, RealmModel realm, UserModel user, UserCredentialModel... input) {
        return CredentialValidation.validCredentials(session, realm, user, input);
    }

    @Override
    public CredentialValidationOutput validCredentials(KeycloakSession session, RealmModel realm, UserCredentialModel... input) {
        // Not supported yet
        return null;
    }

    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        String clientId = consent.getClient().getId();
        if (getConsentEntityByClientId(user, clientId) != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + user.getId() + "]");
        }

        MongoUserConsentEntity consentEntity = new MongoUserConsentEntity();
        consentEntity.setUserId(user.getId());
        consentEntity.setClientId(clientId);
        fillEntityFromModel(consent, consentEntity);
        getMongoStore().insertEntity(consentEntity, invocationContext);
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientId) {
        UserConsentEntity consentEntity = getConsentEntityByClientId(user, clientId);
        return consentEntity!=null ? toConsentModel(realm, consentEntity) : null;
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        List<UserConsentModel> result = new ArrayList<UserConsentModel>();

        DBObject query = new QueryBuilder()
                .and("userId").is(user.getId())
                .get();
        List<MongoUserConsentEntity> grantedConsents = getMongoStore().loadEntities(MongoUserConsentEntity.class, query, invocationContext);

        for (UserConsentEntity consentEntity : grantedConsents) {
            UserConsentModel model = toConsentModel(realm, consentEntity);
            result.add(model);
        }

        return result;
    }

    private MongoUserConsentEntity getConsentEntityByClientId(UserModel user, String clientId) {
        DBObject query = new QueryBuilder()
                .and("userId").is(user.getId())
                .and("clientId").is(clientId)
                .get();
        return getMongoStore().loadSingleEntity(MongoUserConsentEntity.class, query, invocationContext);
    }

    private UserConsentModel toConsentModel(RealmModel realm, UserConsentEntity entity) {
        ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);

        for (String roleId : entity.getGrantedRoles()) {
            RoleModel roleModel = realm.getRoleById(roleId);
            if (roleModel != null) {
                model.addGrantedRole(roleModel);
            }
        }

        for (String protMapperId : entity.getGrantedProtocolMappers()) {
            ProtocolMapperModel protocolMapper = client.getProtocolMapperById(protMapperId);
            model.addGrantedProtocolMapper(protocolMapper);
        }
        return model;
    }

    // Fill roles and protocolMappers to entity
    private void fillEntityFromModel(UserConsentModel consent, MongoUserConsentEntity consentEntity) {
        List<String> roleIds = new LinkedList<String>();
        for (RoleModel role : consent.getGrantedRoles()) {
            roleIds.add(role.getId());
        }
        consentEntity.setGrantedRoles(roleIds);

        List<String> protMapperIds = new LinkedList<String>();
        for (ProtocolMapperModel protMapperModel : consent.getGrantedProtocolMappers()) {
            protMapperIds.add(protMapperModel.getId());
        }
        consentEntity.setGrantedProtocolMappers(protMapperIds);
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        String clientId = consent.getClient().getId();
        MongoUserConsentEntity consentEntity = getConsentEntityByClientId(user, clientId);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + user.getId() + "]");
        } else {
            fillEntityFromModel(consent, consentEntity);
            getMongoStore().updateEntity(consentEntity, invocationContext);
        }
    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientId) {
        MongoUserConsentEntity entity = getConsentEntityByClientId(user, clientId);
        if (entity == null) {
            return false;
        }

        return getMongoStore().removeEntity(entity, invocationContext);
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {

    }
}
