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

package org.keycloak.models.jpa;

import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
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
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.UserConsentClientScopeEntity;
import org.keycloak.models.jpa.entities.UserConsentEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.LockModeType;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("JpaQueryApiInspection")
public class JpaUserProvider implements UserProvider, UserCredentialStore {

    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private final KeycloakSession session;
    protected EntityManager em;
    private final JpaUserCredentialStore credentialStore;

    public JpaUserProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
        credentialStore = new JpaUserCredentialStore(session, em);
    }

    @Override
    public UserModel addUser(RealmModel realm, String id, String username, boolean addDefaultRoles, boolean addDefaultRequiredActions) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }

        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setCreatedTimestamp(System.currentTimeMillis());
        entity.setUsername(username.toLowerCase());
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        UserAdapter userModel = new UserAdapter(session, realm, em, entity);

        if (addDefaultRoles) {
            DefaultRoles.addDefaultRoles(realm, userModel);

            for (GroupModel g : realm.getDefaultGroups()) {
                userModel.joinGroupImpl(g); // No need to check if user has group as it's new user
            }
        }

        if (addDefaultRequiredActions){
            for (RequiredActionProviderModel r : realm.getRequiredActionProviders()) {
                if (r.isEnabled() && r.isDefaultAction()) {
                    userModel.addRequiredAction(r.getAlias());
                }
            }
        }

        return userModel;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return addUser(realm, KeycloakModelUtils.generateId(), username.toLowerCase(), true, true);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserEntity userEntity = em.find(UserEntity.class, user.getId());
        if (userEntity == null) return false;
        removeUser(userEntity);
        return true;
    }

    private void removeUser(UserEntity user) {
        String id = user.getId();
        em.createNamedQuery("deleteUserRoleMappingsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserGroupMembershipsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteFederatedIdentityByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentClientScopesByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByUser").setParameter("user", user).executeUpdate();
        em.flush();
        // not sure why i have to do a clear() here.  I was getting some messed up errors that Hibernate couldn't
        // un-delete the UserEntity.
        em.clear();
        user = em.find(UserEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (user != null) {
            em.remove(user);
        }

        em.flush();
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel identity) {
        FederatedIdentityEntity entity = new FederatedIdentityEntity();
        entity.setRealmId(realm.getId());
        entity.setIdentityProvider(identity.getIdentityProvider());
        entity.setUserId(identity.getUserId());
        entity.setUserName(identity.getUserName().toLowerCase());
        entity.setToken(identity.getToken());
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userEntity);
        em.persist(entity);
        em.flush();
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        FederatedIdentityEntity federatedIdentity = findFederatedIdentity(federatedUser, federatedIdentityModel.getIdentityProvider(), LockModeType.PESSIMISTIC_WRITE);

        federatedIdentity.setToken(federatedIdentityModel.getToken());

        em.persist(federatedIdentity);
        em.flush();
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String identityProvider) {
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider, LockModeType.PESSIMISTIC_WRITE);
        if (entity != null) {
            em.remove(entity);
            em.flush();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId, LockModeType.NONE);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + userId + "]");
        }

        long currentTime = Time.currentTimeMillis();

        consentEntity = new UserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUser(em.getReference(UserEntity.class, userId));
        StorageId clientStorageId = new StorageId(clientId);
        if (clientStorageId.isLocal()) {
            consentEntity.setClientId(clientId);
        } else {
            consentEntity.setClientStorageProvider(clientStorageId.getProviderId());
            consentEntity.setExternalClientId(clientStorageId.getExternalId());
        }

        consentEntity.setCreatedDate(currentTime);
        consentEntity.setLastUpdatedDate(currentTime);
        em.persist(consentEntity);
        em.flush();

        updateGrantedConsentEntity(consentEntity, consent);
    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, String userId, String clientId) {
        UserConsentEntity entity = getGrantedConsentEntity(userId, clientId, LockModeType.NONE);
        return toConsentModel(realm, entity);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, String userId) {
        TypedQuery<UserConsentEntity> query = em.createNamedQuery("userConsentsByUser", UserConsentEntity.class);
        query.setParameter("userId", userId);
        List<UserConsentEntity> results = query.getResultList();

        List<UserConsentModel> consents = new ArrayList<UserConsentModel>();
        for (UserConsentEntity entity : results) {
            UserConsentModel model = toConsentModel(realm, entity);
            consents.add(model);
        }
        return consents;
    }

    @Override
    public void updateConsent(RealmModel realm, String userId, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + userId + "]");
        }

        updateGrantedConsentEntity(consentEntity, consent);
    }

    public boolean revokeConsentForClient(RealmModel realm, String userId, String clientId) {
        UserConsentEntity consentEntity = getGrantedConsentEntity(userId, clientId, LockModeType.PESSIMISTIC_WRITE);
        if (consentEntity == null) return false;

        em.remove(consentEntity);
        em.flush();
        return true;
    }


    private UserConsentEntity getGrantedConsentEntity(String userId, String clientId, LockModeType lockMode) {
        StorageId clientStorageId = new StorageId(clientId);
        String queryName = clientStorageId.isLocal() ?  "userConsentByUserAndClient" : "userConsentByUserAndExternalClient";
        TypedQuery<UserConsentEntity> query = em.createNamedQuery(queryName, UserConsentEntity.class);
        query.setParameter("userId", userId);
        if (clientStorageId.isLocal()) {
            query.setParameter("clientId", clientId);
        } else {
            query.setParameter("clientStorageProvider", clientStorageId.getProviderId());
            query.setParameter("externalClientId", clientStorageId.getExternalId());
        }
        query.setLockMode(lockMode);
        List<UserConsentEntity> results = query.getResultList();
        if (results.size() > 1) {
            throw new ModelException("More results found for user [" + userId + "] and client [" + clientId + "]");
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            return null;
        }

    }

    private UserConsentModel toConsentModel(RealmModel realm, UserConsentEntity entity) {
        if (entity == null) {
            return null;
        }

        StorageId clientStorageId = null;
        if ( entity.getClientId() == null) {
            clientStorageId = new StorageId(entity.getClientStorageProvider(), entity.getExternalClientId());
        } else {
            clientStorageId = new StorageId(entity.getClientId());
        }

        ClientModel client = realm.getClientById(clientStorageId.getId());
        if (client == null) {
            throw new ModelException("Client with id " + clientStorageId.getId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);
        model.setCreatedDate(entity.getCreatedDate());
        model.setLastUpdatedDate(entity.getLastUpdatedDate());

        Collection<UserConsentClientScopeEntity> grantedClientScopeEntities = entity.getGrantedClientScopes();
        if (grantedClientScopeEntities != null) {
            for (UserConsentClientScopeEntity grantedClientScope : grantedClientScopeEntities) {
                ClientScopeModel grantedClientScopeModel = KeycloakModelUtils.findClientScopeById(realm, client, grantedClientScope.getScopeId());
                if (grantedClientScopeModel != null) {
                    model.addGrantedClientScope(grantedClientScopeModel);
                }
            }
        }

        return model;
    }

    // Update roles and protocolMappers to given consentEntity from the consentModel
    private void updateGrantedConsentEntity(UserConsentEntity consentEntity, UserConsentModel consentModel) {
        Collection<UserConsentClientScopeEntity> grantedClientScopeEntities = consentEntity.getGrantedClientScopes();
        Collection<UserConsentClientScopeEntity> scopesToRemove = new HashSet<>(grantedClientScopeEntities);

        for (ClientScopeModel clientScope : consentModel.getGrantedClientScopes()) {
            UserConsentClientScopeEntity grantedClientScopeEntity = new UserConsentClientScopeEntity();
            grantedClientScopeEntity.setUserConsent(consentEntity);
            grantedClientScopeEntity.setScopeId(clientScope.getId());

            // Check if it's already there
            if (!grantedClientScopeEntities.contains(grantedClientScopeEntity)) {
                em.persist(grantedClientScopeEntity);
                em.flush();
                grantedClientScopeEntities.add(grantedClientScopeEntity);
            } else {
                scopesToRemove.remove(grantedClientScopeEntity);
            }
        }
        // Those client scopes were no longer on consentModel and will be removed
        for (UserConsentClientScopeEntity toRemove : scopesToRemove) {
            grantedClientScopeEntities.remove(toRemove);
            em.remove(toRemove);
        }

        consentEntity.setLastUpdatedDate(Time.currentTimeMillis());

        em.flush();
    }


    @Override
    public void setNotBeforeForUser(RealmModel realm, UserModel user, int notBefore) {
        UserEntity entity = em.getReference(UserEntity.class, user.getId());
        entity.setNotBefore(notBefore);
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        UserEntity entity = em.getReference(UserEntity.class, user.getId());
        return entity.getNotBefore();
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        int num = em.createNamedQuery("grantRoleToAllUsers")
                .setParameter("realmId", realm.getId())
                .setParameter("roleId", role.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm) {
        int num = em.createNamedQuery("deleteUserConsentClientScopesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedIdentityByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteCredentialsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserAttributesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserGroupMembershipByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUsersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        int num = em.createNamedQuery("deleteUserRoleMappingsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserRequiredActionsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteFederatedIdentityByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteCredentialsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserAttributesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserGroupMembershipsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserConsentClientScopesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUserConsentsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        num = em.createNamedQuery("deleteUsersByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
    }

    @Override
    public void unlinkUsers(RealmModel realm, String storageProviderId) {
        em.createNamedQuery("unlinkUsers")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        em.createNamedQuery("deleteUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        StorageId clientStorageId = new StorageId(client.getId());
        if (clientStorageId.isLocal()) {
            int num = em.createNamedQuery("deleteUserConsentClientScopesByClient")
                    .setParameter("clientId", client.getId())
                    .executeUpdate();
            num = em.createNamedQuery("deleteUserConsentsByClient")
                    .setParameter("clientId", client.getId())
                    .executeUpdate();
        } else {
            em.createNamedQuery("deleteUserConsentClientScopesByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId", clientStorageId.getExternalId())
                    .executeUpdate();
            em.createNamedQuery("deleteUserConsentsByExternalClient")
                    .setParameter("clientStorageProvider", clientStorageId.getProviderId())
                    .setParameter("externalClientId", clientStorageId.getExternalId())
                    .executeUpdate();

        }
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        // No-op
    }

    @Override
    public void preRemove(ClientScopeModel clientScope) {
        em.createNamedQuery("deleteUserConsentClientScopesByClientScope")
                .setParameter("scopeId", clientScope.getId())
                .executeUpdate();
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role) {
        TypedQuery<UserEntity> query = em.createNamedQuery("usersInRole", UserEntity.class);
        query.setParameter("roleId", role.getId());
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }


    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        UserEntity userEntity = em.find(UserEntity.class, id);
        if (userEntity == null || !realm.getId().equals(userEntity.getRealmId())) return null;
        return new UserAdapter(session, realm, em, userEntity);
    }

    @Override
    public UserModel getUserByUsername(String username, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", username.toLowerCase());
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();
        if (results.size() == 0) return null;
        return new UserAdapter(session, realm, em, results.get(0));
    }

    @Override
    public UserModel getUserByEmail(String email, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByEmail", UserEntity.class);
        query.setParameter("email", email.toLowerCase());
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();

        if (results.isEmpty()) return null;

        ensureEmailConstraint(results, realm);

        return new UserAdapter(session, realm, em, results.get(0));
    }

     @Override
    public void close() {
    }

    @Override
    public UserModel getUserByFederatedIdentity(FederatedIdentityModel identity, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("findUserByFederatedIdentityAndRealm", UserEntity.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("identityProvider", identity.getIdentityProvider());
        query.setParameter("userId", identity.getUserId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for identityProvider=" + identity.getIdentityProvider() +
                    ", userId=" + identity.getUserId() + ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(session, realm, em, user);
        }
    }

    @Override
    public UserModel getServiceAccount(ClientModel client) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByServiceAccount", UserEntity.class);
        query.setParameter("realmId", client.getRealm().getId());
        query.setParameter("clientInternalId", client.getId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More service account linked users found for client=" + client.getClientId() +
                    ", results=" + results);
        } else {
            UserEntity user = results.get(0);
            return new UserAdapter(session, client.getRealm(), em, user);
        }
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, boolean includeServiceAccounts) {
        return getUsers(realm, -1, -1, includeServiceAccounts);
    }

    @Override
    public int getUsersCount(RealmModel realm, boolean includeServiceAccount) {
        String namedQuery = "getRealmUserCountExcludeServiceAccount";

        if (includeServiceAccount) {
            namedQuery = "getRealmUserCount";
        }

        Object count = em.createNamedQuery(namedQuery)
                .setParameter("realmId", realm.getId())
                .getSingleResult();
        return ((Number)count).intValue();
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return getUsersCount(realm, false);
    }

    @Override
    public int getUsersCount(RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }

        TypedQuery<Long> query = em.createNamedQuery("userCountInGroups", Long.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("groupIds", groupIds);
        Long count = query.getSingleResult();

        return count.intValue();
    }

    @Override
    public int getUsersCount(String search, RealmModel realm) {
        TypedQuery<Long> query = em.createNamedQuery("searchForUserCount", Long.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        Long count = query.getSingleResult();

        return count.intValue();
    }

    @Override
    public int getUsersCount(String search, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }

        TypedQuery<Long> query = em.createNamedQuery("searchForUserCountInGroups", Long.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        query.setParameter("groupIds", groupIds);
        Long count = query.getSingleResult();

        return count.intValue();
    }

    @Override
    public int getUsersCount(Map<String, String> params, RealmModel realm) {
        CriteriaBuilder qb = em.getCriteriaBuilder();
        CriteriaQuery<Long> userQuery = qb.createQuery(Long.class);
        Root<UserEntity> from = userQuery.from(UserEntity.class);
        Expression<Long> count = qb.count(from);

        userQuery = userQuery.select(count);
        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(qb.equal(from.get("realmId"), realm.getId()));

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }

            switch (key) {
                case UserModel.USERNAME:
                    restrictions.add(qb.like(from.get("username"), "%" + value + "%"));
                    break;
                case UserModel.FIRST_NAME:
                    restrictions.add(qb.like(from.get("firstName"), "%" + value + "%"));
                    break;
                case UserModel.LAST_NAME:
                    restrictions.add(qb.like(from.get("lastName"), "%" + value + "%"));
                    break;
                case UserModel.EMAIL:
                    restrictions.add(qb.like(from.get("email"), "%" + value + "%"));
                    break;
            }
        }

        userQuery = userQuery.where(restrictions.toArray(new Predicate[0]));
        TypedQuery<Long> query = em.createQuery(userQuery);
        Long result = query.getSingleResult();

        return result.intValue();
    }

    @Override
    public int getUsersCount(Map<String, String> params, RealmModel realm, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }

        CriteriaBuilder qb = em.getCriteriaBuilder();
        CriteriaQuery<Long> userQuery = qb.createQuery(Long.class);
        Root<UserGroupMembershipEntity> from = userQuery.from(UserGroupMembershipEntity.class);
        Expression<Long> count = qb.count(from.get("user"));
        userQuery = userQuery.select(count);

        List<Predicate> restrictions = new ArrayList<>();
        restrictions.add(qb.equal(from.get("user").get("realmId"), realm.getId()));
        restrictions.add(from.get("groupId").in(groupIds));

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }

            switch (key) {
                case UserModel.USERNAME:
                    restrictions.add(qb.like(from.get("user").get("username"), "%" + value + "%"));
                    break;
                case UserModel.FIRST_NAME:
                    restrictions.add(qb.like(from.get("user").get("firstName"), "%" + value + "%"));
                    break;
                case UserModel.LAST_NAME:
                    restrictions.add(qb.like(from.get("user").get("lastName"), "%" + value + "%"));
                    break;
                case UserModel.EMAIL:
                    restrictions.add(qb.like(from.get("user").get("email"), "%" + value + "%"));
                    break;
            }
        }

        userQuery = userQuery.where(restrictions.toArray(new Predicate[0]));
        TypedQuery<Long> query = em.createQuery(userQuery);
        Long result = query.getSingleResult();

        return result.intValue();
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
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults, boolean includeServiceAccounts) {
        String queryName = includeServiceAccounts ? "getAllUsersByRealm" : "getAllUsersByRealmExcludeServiceAccount" ;

        TypedQuery<UserEntity> query = em.createNamedQuery(queryName, UserEntity.class);
        query.setParameter("realmId", realm.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new LinkedList<>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("usersInRole", UserEntity.class);
        query.setParameter("roleId", role.getId());
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new LinkedList<>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
        return searchForUser(search, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("searchForUser", UserEntity.class);
        query.setParameter("realmId", realm.getId());
        query.setParameter("search", "%" + search.toLowerCase() + "%");
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new LinkedList<>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm) {
        return searchForUser(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> queryBuilder = builder.createQuery(UserEntity.class);
        Root<UserEntity> root = queryBuilder.from(UserEntity.class);

        List<Predicate> predicates = new ArrayList();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        if (!session.getAttributeOrDefault(UserModel.INCLUDE_SERVICE_ACCOUNT, true)) {
            predicates.add(root.get("serviceAccountClientLink").isNull());
        }

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null) {
                continue;
            }

            switch (key) {
                case UserModel.USERNAME:
                case UserModel.FIRST_NAME:
                case UserModel.LAST_NAME:
                case UserModel.EMAIL:
                    predicates.add(builder.like(builder.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
            }
        }

        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);

        if (userGroups != null) {
            Subquery subquery = queryBuilder.subquery(String.class);
            Root<UserGroupMembershipEntity> from = subquery.from(UserGroupMembershipEntity.class);

            subquery.select(builder.literal(1));

            List<Predicate> subPredicates = new ArrayList<>();

            subPredicates.add(from.get("groupId").in(userGroups));
            subPredicates.add(builder.equal(from.get("user").get("id"), root.get("id")));

            Subquery subquery1 = queryBuilder.subquery(String.class);

            subquery1.select(builder.literal(1));
            Root from1 = subquery1.from(ResourceEntity.class);

            List<Predicate> subs = new ArrayList<>();

            Expression<String> groupId = from.get("groupId");
            subs.add(builder.like(from1.get("name"), builder.concat("group.resource.", groupId)));

            subquery1.where(subs.toArray(new Predicate[subs.size()]));

            subPredicates.add(builder.exists(subquery1));

            subquery.where(subPredicates.toArray(new Predicate[subPredicates.size()]));

            predicates.add(builder.exists(subquery));
        }

        queryBuilder.where(predicates.toArray(new Predicate[predicates.size()])).orderBy(builder.asc(root.get(UserModel.USERNAME)));

        TypedQuery<UserEntity> query = em.createQuery(queryBuilder);

        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }

        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }

        List<UserModel> results = new ArrayList<>();
        UserProvider users = session.users();

        for (UserEntity entity : query.getResultList()) {
            results.add(users.getUserById(entity.getId(), realm));
        }

        return results;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUsersByAttributeNameAndValue", UserEntity.class);
        query.setParameter("name", attrName);
        query.setParameter("value", attrValue);
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity user : results) {
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    private FederatedIdentityEntity findFederatedIdentity(UserModel user, String identityProvider, LockModeType lockMode) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUserAndProvider", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("identityProvider", identityProvider);
        query.setLockMode(lockMode);
        List<FederatedIdentityEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }


    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUser", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        List<FederatedIdentityEntity> results = query.getResultList();
        Set<FederatedIdentityModel> set = new HashSet<FederatedIdentityModel>();
        for (FederatedIdentityEntity entity : results) {
            set.add(new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken()));
        }
        return set;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String identityProvider, RealmModel realm) {
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider, LockModeType.NONE);
        return (entity != null) ? new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken()) : null;
    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel component) {
        if (component.getProviderType().equals(UserStorageProvider.class.getName())) {
            removeImportedUsers(realm, component.getId());
        }
        if (component.getProviderType().equals(ClientStorageProvider.class.getName())) {
            removeConsentByClientStorageProvider(realm, component.getId());
        }
    }

    protected void removeConsentByClientStorageProvider(RealmModel realm, String providerId) {
        em.createNamedQuery("deleteUserConsentClientScopesByClientStorageProvider")
                .setParameter("clientStorageProvider", providerId)
                .executeUpdate();
        em.createNamedQuery("deleteUserConsentsByClientStorageProvider")
                .setParameter("clientStorageProvider", providerId)
                .executeUpdate();

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        credentialStore.updateCredential(realm, user, cred);
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel cred) {
        CredentialEntity entity = credentialStore.createCredentialEntity(realm, user, cred);

        UserEntity userEntity = userInEntityManagerContext(user.getId());
        if (userEntity != null) {
            userEntity.getCredentials().add(entity);
        }
        return toModel(entity);
    }

    @Override
    public boolean removeStoredCredential(RealmModel realm, UserModel user, String id) {
        CredentialEntity entity = credentialStore.removeCredentialEntity(realm, user, id);
        UserEntity userEntity = userInEntityManagerContext(user.getId());
        if (entity != null && userEntity != null) {
            userEntity.getCredentials().remove(entity);
        }
        return entity != null;
    }

    @Override
    public CredentialModel getStoredCredentialById(RealmModel realm, UserModel user, String id) {
        return credentialStore.getStoredCredentialById(realm, user, id);
    }

    protected CredentialModel toModel(CredentialEntity entity) {
        return credentialStore.toModel(entity);
    }

    @Override
    public List<CredentialModel> getStoredCredentials(RealmModel realm, UserModel user) {
        return credentialStore.getStoredCredentials(realm, user);
    }

    @Override
    public List<CredentialModel> getStoredCredentialsByType(RealmModel realm, UserModel user, String type) {
        List<CredentialEntity> results;
        UserEntity userEntity = userInEntityManagerContext(user.getId());
        if (userEntity != null) {

            // user already in persistence context, no need to execute a query
            results = userEntity.getCredentials().stream().filter(it -> type.equals(it.getType()))
                    .sorted(Comparator.comparingInt(CredentialEntity::getPriority))
                    .collect(Collectors.toList());
            List<CredentialModel> rtn = new LinkedList<>();
            for (CredentialEntity entity : results) {
                rtn.add(toModel(entity));
            }
            return rtn;
        } else {
           return credentialStore.getStoredCredentialsByType(realm, user, type);
        }
    }

    @Override
    public CredentialModel getStoredCredentialByNameAndType(RealmModel realm, UserModel user, String name, String type) {
        return credentialStore.getStoredCredentialByNameAndType(realm, user, name, type);
    }

    @Override
    public boolean moveCredentialTo(RealmModel realm, UserModel user, String id, String newPreviousCredentialId) {
        return credentialStore.moveCredentialTo(realm, user, id, newPreviousCredentialId);
    }

    // Could override this to provide a custom behavior.
    protected void ensureEmailConstraint(List<UserEntity> users, RealmModel realm) {
        UserEntity user = users.get(0);

        if (users.size() > 1) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them
            // but duplicates haven't been removed.
            throw new ModelDuplicateException("Multiple users with email '" + user.getEmail() + "' exist in Keycloak.");
        }

        if (realm.isDuplicateEmailsAllowed()) {
            return;
        }

        if (user.getEmail() != null && !user.getEmail().equals(user.getEmailConstraint())) {
            // Realm settings have been changed from allowing duplicate emails to not allowing them.
            // We need to update the email constraint to reflect this change in the user entities.
            user.setEmailConstraint(user.getEmail());
            em.persist(user);
        }
    }

    private UserEntity userInEntityManagerContext(String id) {
        UserEntity user = em.getReference(UserEntity.class, id);
        boolean isLoaded = em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(user);
        return isLoaded ? user : null;
    }
}
