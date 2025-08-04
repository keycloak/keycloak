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

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.storage.jpa.JpaHashUtils.predicateForFilteringUsersByAttributes;
import static org.keycloak.storage.jpa.JpaHashUtils.predicateForMultiFilteringUsersByAttributes;
import static org.keycloak.utils.StreamsUtil.closing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.keycloak.authorization.jpa.entities.ResourceEntity;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.UserCredentialStore;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserRoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.CredentialEntity;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserConsentClientScopeEntity;
import org.keycloak.models.jpa.entities.UserConsentEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.FilterAttributeRepresentation;
import org.keycloak.representations.idm.UserRolesBodyRepresentation;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.jpa.JpaHashUtils;
import org.keycloak.utils.StringUtil;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings("JpaQueryApiInspection")
public class JpaUserProvider implements UserProvider, UserCredentialStore {

    private static final String EMAIL = "email";
    private static final String EMAIL_VERIFIED = "emailVerified";
    private static final String USERNAME = "username";
    private static final String ID = "id";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    private static final char ESCAPE_BACKSLASH = '\\';

    private final KeycloakSession session;
    private final JpaUserCredentialStore credentialStore;
    protected EntityManager em;

    public JpaUserProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
        credentialStore = new JpaUserCredentialStore(session, em);
    }

    private static List<FilterAttributeRepresentation> attributesMapToRepresentation(Map<String, String> attributes) {
        Boolean exact = Boolean.parseBoolean(attributes.get(UserModel.EXACT));
        List<FilterAttributeRepresentation> attributeRepresentations = new ArrayList<>();
        attributes.forEach((key, value) -> {
            FilterAttributeRepresentation representation = new FilterAttributeRepresentation();
            representation.setKey(key);
            representation.setValues(List.of(value));
            representation.setExact(exact);
            attributeRepresentations.add(representation);
        });
        return attributeRepresentations;
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
            userModel.grantRole(realm.getDefaultRole());

            // No need to check if user has group as it's new user
            realm.getDefaultGroupsStream().forEach(userModel::joinGroupImpl);
        }

        if (addDefaultRequiredActions) {
            realm.getRequiredActionProvidersStream()
                .filter(RequiredActionProviderModel::isEnabled)
                .filter(RequiredActionProviderModel::isDefaultAction)
                .map(RequiredActionProviderModel::getAlias)
                .forEach(userModel::addRequiredAction);
        }

        return userModel;
    }

    @Override
    public UserModel addUser(RealmModel realm, String username) {
        return addUser(realm, KeycloakModelUtils.generateId(), username.toLowerCase(), true, true);
    }

    @Override
    public boolean removeUser(RealmModel realm, UserModel user) {
        UserEntity userEntity = em.find(UserEntity.class, user.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (userEntity == null) return false;
        removeUser(userEntity);
        return true;
    }

    private void removeUser(UserEntity user) {
        String id = user.getId();
        em.createNamedQuery("deleteUserRoleMappingsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserGroupMembershipsByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentClientScopesByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByUser").setParameter("user", user).executeUpdate();

        em.remove(user);
        em.flush();
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel identity) {
        FederatedIdentityEntity entity = new FederatedIdentityEntity();
        entity.setRealmId(realm.getId());
        entity.setIdentityProvider(identity.getIdentityProvider());
        entity.setUserId(identity.getUserId());
        entity.setUserName(identity.getUserName());
        entity.setToken(identity.getToken());
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        entity.setUser(userEntity);
        em.persist(entity);
        em.flush();
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel federatedUser, FederatedIdentityModel federatedIdentityModel) {
        FederatedIdentityEntity federatedIdentity = findFederatedIdentity(federatedUser, federatedIdentityModel.getIdentityProvider(), LockModeType.PESSIMISTIC_WRITE);

        federatedIdentity.setUserName(federatedIdentityModel.getUserName());
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
    public void preRemove(RealmModel realm, IdentityProviderModel provider) {
        em.createNamedQuery("deleteFederatedIdentityByProvider")
                .setParameter("realmId", realm.getId())
                .setParameter("providerAlias", provider.getAlias()).executeUpdate();
    }

    @Override
    public void addConsent(RealmModel realm, String userId, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        long currentTime = Time.currentTimeMillis();

        UserConsentEntity consentEntity = new UserConsentEntity();
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
    public Stream<UserConsentModel> getConsentsStream(RealmModel realm, String userId) {
        TypedQuery<UserConsentEntity> query = em.createNamedQuery("userConsentsByUser", UserConsentEntity.class);
        query.setParameter("userId", userId);
        return closing(query.getResultStream().map(entity -> toConsentModel(realm, entity)));
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

    @Override
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

        StorageId clientStorageId;
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
        if (entity == null) {
            throw new ModelException("User does not exists");
        }
        entity.setNotBefore(notBefore);
    }

    @Override
    public int getNotBeforeOfUser(RealmModel realm, UserModel user) {
        UserEntity entity = em.getReference(UserEntity.class, user.getId());
        if (entity == null) {
            throw new ModelException("User does not exists");
        }
        return entity.getNotBefore();
    }

    @Override
    public void grantToAllUsers(RealmModel realm, RoleModel role) {
        if (realm.equals(role.isClientRole() ? ((ClientModel)role.getContainer()).getRealm() : (RealmModel)role.getContainer())) {
            em.createNamedQuery("grantRoleToAllUsers")
                .setParameter("realmId", realm.getId())
                .setParameter("roleId", role.getId())
                .executeUpdate();
        }
    }

    @Override
    public void preRemove(RealmModel realm) {
        em.createNamedQuery("deleteUserConsentClientScopesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedIdentityByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteCredentialsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUserAttributesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUserGroupMembershipByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteUsersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void removeImportedUsers(RealmModel realm, String storageProviderId) {
        em.createNamedQuery("deleteUserRoleMappingsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUserRequiredActionsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteFederatedIdentityByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteCredentialsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUserAttributesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUserGroupMembershipsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUserConsentClientScopesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUserConsentsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", storageProviderId)
                .executeUpdate();
        em.createNamedQuery("deleteUsersByRealmAndLink")
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
            em.createNamedQuery("deleteUserConsentClientScopesByClient")
                    .setParameter("clientId", client.getId())
                    .executeUpdate();
            em.createNamedQuery("deleteUserConsentsByClient")
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
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());
        return closing(query.getResultStream().map(entity -> new UserAdapter(session, realm, em, entity)));
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role) {
        TypedQuery<UserEntity> query = em.createNamedQuery("usersInRole", UserEntity.class);
        query.setParameter("roleId", role.getId());
        return closing(query.getResultStream().map(entity -> new UserAdapter(session, realm, em, entity)));
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();

    }

    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        UserEntity userEntity = em.find(UserEntity.class, id);
        if (userEntity == null || !realm.getId().equals(userEntity.getRealmId())) return null;
        return new UserAdapter(session, realm, em, userEntity);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserByUsername", UserEntity.class);
        query.setParameter("username", username.toLowerCase());
        query.setParameter("realmId", realm.getId());
        List<UserEntity> results = query.getResultList();
        if (results.isEmpty()) return null;
        return new UserAdapter(session, realm, em, results.get(0));
    }

    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
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
    public UserModel getUserByFederatedIdentity(RealmModel realm, FederatedIdentityModel identity) {
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
    public int getUsersCount(RealmModel realm, String search) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<UserEntity> root = queryBuilder.from(UserEntity.class);

        queryBuilder.select(builder.count(root));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        for (String stringToSearch : search.trim().split("\\s+")) {
            predicates.add(builder.or(getSearchOptionPredicateArray(stringToSearch, builder, root)));
        }

        queryBuilder.where(predicates.toArray(Predicate[]::new));

        return em.createQuery(queryBuilder).getSingleResult().intValue();
    }

    @Override
    public int getUsersCount(RealmModel realm, String search, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);

        Root<UserGroupMembershipEntity> groupMembership = queryBuilder.from(UserGroupMembershipEntity.class);
        Join<UserGroupMembershipEntity, UserEntity> userJoin = groupMembership.join("user");

        queryBuilder.select(builder.count(userJoin));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(userJoin.get("realmId"), realm.getId()));

        for (String stringToSearch : search.trim().split("\\s+")) {
            predicates.add(builder.or(getSearchOptionPredicateArray(stringToSearch, builder, userJoin)));
        }

        predicates.add(groupMembership.get("groupId").in(groupIds));

        queryBuilder.where(predicates.toArray(Predicate[]::new));

        return em.createQuery(queryBuilder).getSingleResult().intValue();
    }

    @Override
    public int getUsersCount(RealmModel realm, Map<String, String> params) {
        CriteriaBuilder qb = em.getCriteriaBuilder();
        CriteriaQuery<Long> userQuery = qb.createQuery(Long.class);
        Root<UserEntity> from = userQuery.from(UserEntity.class);
        Expression<Long> count = qb.count(from);

        userQuery = userQuery.select(count);
        List<FilterAttributeRepresentation> attributeRepresentations = attributesMapToRepresentation(params);
        List<Predicate> restrictions = predicates(attributeRepresentations, from, Map.of());
        restrictions.add(qb.equal(from.get("realmId"), realm.getId()));

        userQuery = userQuery.where(restrictions.toArray(Predicate[]::new));
        TypedQuery<Long> query = em.createQuery(userQuery);
        Long result = query.getSingleResult();

        return result.intValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public int getUsersCount(RealmModel realm, Map<String, String> params, Set<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return 0;
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<UserEntity> root = countQuery.from(UserEntity.class);
        countQuery.select(cb.count(root));

        List<FilterAttributeRepresentation> attributeRepresentations = attributesMapToRepresentation(params);
        List<Predicate> restrictions = predicates(attributeRepresentations, root, Map.of());
        restrictions.add(cb.equal(root.get("realmId"), realm.getId()));

        groupsWithPermissionsSubquery(countQuery, groupIds, root, restrictions);

        countQuery.where(restrictions.toArray(Predicate[]::new));
        TypedQuery<Long> query = em.createQuery(countQuery);
        Long result = query.getSingleResult();

        return result.intValue();
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("groupMembership", UserEntity.class);
        query.setParameter("groupId", group.getId());

        return closing(paginateQuery(query, firstResult, maxResults).getResultStream().map(user -> new UserAdapter(session, realm, em, user)));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, String search, Boolean exact, Integer first, Integer max) {
        TypedQuery<UserEntity> query;
        if (StringUtil.isBlank(search)) {
            query = em.createNamedQuery("groupMembership", UserEntity.class);
        } else if (Boolean.TRUE.equals(exact)) {
            query = em.createNamedQuery("groupMembershipByUser", UserEntity.class);
            query.setParameter("search", search);
        } else {
            query = em.createNamedQuery("groupMembershipByUserContained", UserEntity.class);
            query.setParameter("search", search.toLowerCase());
        }
        query.setParameter("groupId", group.getId());

        return closing(paginateQuery(query, first, max).getResultStream().map(user -> new UserAdapter(session, realm, em, user)));
    }

    @Override
    public Stream<UserModel> getRoleMembersStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        TypedQuery<UserEntity> query = em.createNamedQuery("usersInRole", UserEntity.class);
        query.setParameter("roleId", role.getId());

        final UserProvider users = session.users();
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(userEntity -> users.getUserById(realm, userEntity.getId()))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        Map<String, String> attributes = new HashMap<>(2);
        attributes.put(UserModel.SEARCH, search);
        attributes.put(UserModel.INCLUDE_SERVICE_ACCOUNT, Boolean.FALSE.toString());

        return searchForUserStream(realm, attributes, firstResult, maxResults);
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        return getBaseQuery(realm, attributes, firstResult, maxResults);
    }

    public Stream<UserRoleModel> searchForUserRoleStream(RealmModel realm, UserRolesBodyRepresentation userRolesBodyRepresentation) {
        return getBaseQuery(realm, userRolesBodyRepresentation);
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        boolean longAttribute = attrValue != null && attrValue.length() > 255;
        TypedQuery<UserEntity> query = longAttribute ?
                em.createNamedQuery("getRealmUsersByAttributeNameAndLongValue", UserEntity.class)
                        .setParameter("realmId", realm.getId())
                        .setParameter("name", attrName)
                        .setParameter("longValueHash", JpaHashUtils.hashForAttributeValue(attrValue)):
                em.createNamedQuery("getRealmUsersByAttributeNameAndValue", UserEntity.class)
                        .setParameter("realmId", realm.getId())
                        .setParameter("name", attrName)
                        .setParameter("value", attrValue);

        return closing(query.getResultStream()
                // The following check verifies that there are no collisions with hashes
                .filter(longAttribute ? predicateForFilteringUsersByAttributes(Map.of(attrName, attrValue), JpaHashUtils::compareSourceValue) : u -> true)
                .map(userEntity -> new UserAdapter(session, realm, em, userEntity)));
    }

    private FederatedIdentityEntity findFederatedIdentity(UserModel user, String identityProvider, LockModeType lockMode) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUserAndProvider", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("identityProvider", identityProvider);
        query.setLockMode(lockMode);
        List<FederatedIdentityEntity> results = query.getResultList();
        return !results.isEmpty() ? results.get(0) : null;
    }


    @Override
    public Stream<FederatedIdentityModel> getFederatedIdentitiesStream(RealmModel realm, UserModel user) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUser", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);

        return closing(query.getResultStream().map(entity -> new FederatedIdentityModel(entity.getIdentityProvider(),
                entity.getUserId(), entity.getUserName(), entity.getToken())).distinct());
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(RealmModel realm, UserModel user, String identityProvider) {
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
    public Stream<CredentialModel> getStoredCredentialsStream(RealmModel realm, UserModel user) {
        return credentialStore.getStoredCredentialsStream(realm, user);
    }

    @Override
    public Stream<CredentialModel> getStoredCredentialsByTypeStream(RealmModel realm, UserModel user, String type) {
        UserEntity userEntity = userInEntityManagerContext(user.getId());
        if (userEntity != null) {
            // user already in persistence context, no need to execute a query
            return userEntity.getCredentials().stream().filter(it -> type.equals(it.getType()))
                    .sorted(Comparator.comparingInt(CredentialEntity::getPriority))
                    .map(this::toModel);
        } else {
           return credentialStore.getStoredCredentialsByTypeStream(realm, user, type);
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

    private Predicate[] getSearchOptionPredicateArray(String value, CriteriaBuilder builder, From<?, UserEntity> from) {
        value = value.toLowerCase();

        List<Predicate> orPredicates = new ArrayList<>();

        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            // exact search
            value = value.substring(1, value.length() - 1);

            orPredicates.add(builder.equal(from.get(USERNAME), value));
            orPredicates.add(builder.equal(from.get(EMAIL), value));
            orPredicates.add(builder.equal(builder.lower(from.get(FIRST_NAME)), value));
            orPredicates.add(builder.equal(builder.lower(from.get(LAST_NAME)), value));
        } else {
            value = value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            value = value.replace("*", "%");
            if (value.isEmpty() || value.charAt(value.length() - 1) != '%') value += "%";

            orPredicates.add(builder.like(from.get(USERNAME), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(from.get(EMAIL), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(builder.lower(from.get(FIRST_NAME)), value, ESCAPE_BACKSLASH));
            orPredicates.add(builder.like(builder.lower(from.get(LAST_NAME)), value, ESCAPE_BACKSLASH));
        }

        return orPredicates.toArray(Predicate[]::new);
    }

    private UserEntity userInEntityManagerContext(String id) {
        UserEntity user = em.getReference(UserEntity.class, id);
        return em.contains(user) ? user : null;
    }

    private void processPredicate(List<String> values, Consumer<String> action) {
        values.forEach(value -> action.accept(value.toLowerCase()));
    }

    private List<Predicate> predicates(List<FilterAttributeRepresentation> attributeEntries, From<?, UserEntity> from,
                                       Map<String, List<String>> customLongValueSearchAttributes) {
        CriteriaBuilder builder = em.getCriteriaBuilder();

        List<Predicate> predicates = new ArrayList<>();
        List<Predicate> attributePredicates = new ArrayList<>();

        Join<Object, Object> federatedIdentitiesJoin = null;

        for (FilterAttributeRepresentation entry : attributeEntries) {
            String key = entry.getKey();
            List<String> values = entry.getValues();
            List<Predicate> valuePredicates = new ArrayList<>();

            if (values.isEmpty()) {
                continue;
            }

            boolean isExact = Optional.ofNullable(entry.getExact()).orElse(true);
            switch (key) {
                case UserModel.SEARCH: {
                    processPredicate(values, valueLowerCase -> {
                        for (String stringToSearch : valueLowerCase.trim().split("\\s+")) {
                            valuePredicates.add(builder.or(getSearchOptionPredicateArray(stringToSearch, builder, from)));
                        }
                    });
                    break;
                }
                case ID:
                case FIRST_NAME:
                case LAST_NAME:
                case USERNAME:
                case EMAIL: {
                    processPredicate(values, valueLowerCase -> {
                        if (isExact) {
                            valuePredicates.add(builder.equal(builder.lower(from.get(key)), valueLowerCase));
                        } else {
                            valuePredicates.add(builder.like(builder.lower(from.get(key)), "%" + valueLowerCase + "%"));
                        }
                    });
                    break;
                }
                case EMAIL_VERIFIED:
                    valuePredicates.add(builder.equal(from.get(key), Boolean.valueOf(values.get(0).toLowerCase())));
                    break;
                case UserModel.ENABLED:
                    valuePredicates.add(builder.equal(from.get(key), Boolean.valueOf(values.get(0))));
                    break;
                case UserModel.IDP_ALIAS:
                case UserModel.IDP_USER_ID:
                    if (federatedIdentitiesJoin == null) {
                        federatedIdentitiesJoin = from.join("federatedIdentities");
                    }
                    Join<Object, Object> finalFederatedIdentitiesJoin = federatedIdentitiesJoin;
                    valuePredicates.add(
                        builder.equal(finalFederatedIdentitiesJoin.get(key.equals(UserModel.IDP_USER_ID) ? "userId" : "identityProvider"),
                            values.get(0)));
                    break;
                case UserModel.EXACT:
                    break;
                case UserModel.INCLUDE_SERVICE_ACCOUNT: {
                    if (values.get(0) == null || !Boolean.parseBoolean(values.get(0))) {
                        valuePredicates.add(from.get("serviceAccountClientLink").isNull());
                    }
                    break;
                }
                // All unknown attributes will be assumed as custom attributes
                default: {
                    Join<UserEntity, UserAttributeEntity> attributesJoin = from.join("attributes", JoinType.LEFT);
                    processPredicate(values, valueLowerCase -> {
                        if (valueLowerCase.length() > 255) {
                            customLongValueSearchAttributes.put(key, Collections.singletonList(valueLowerCase));
                            valuePredicates.add(builder.and(builder.equal(attributesJoin.get("name"), key),
                                builder.equal(attributesJoin.get("longValueHashLowerCase"), JpaHashUtils.hashForAttributeValueLowerCase(valueLowerCase))));
                        } else {
                            if (isExact) {
                                valuePredicates.add(builder.and(builder.equal(attributesJoin.get("name"), key),
                                    builder.equal(builder.lower(attributesJoin.get("value")), valueLowerCase)));
                            } else {
                                valuePredicates.add(builder.and(builder.equal(attributesJoin.get("name"), key),
                                    builder.like(builder.lower(attributesJoin.get("value")), "%" + valueLowerCase + "%")));
                            }
                        }
                    });
                    break;
                }
            }
            // This ensures that in cases like exact where we do not add any value predicates, we do not add conditions that cannot be met.
            if (!valuePredicates.isEmpty()) attributePredicates.add(builder.or(valuePredicates.toArray(Predicate[]::new)));
        }

        if (!attributePredicates.isEmpty()) predicates.add(builder.and(attributePredicates.toArray(Predicate[]::new)));

        return predicates;
    }

    private Stream<UserModel> getBaseQuery(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        UserRolesBodyRepresentation userRolesBodyRepresentation = new UserRolesBodyRepresentation();
        userRolesBodyRepresentation.setFilterAttributes(attributesMapToRepresentation(attributes));
        userRolesBodyRepresentation.setFirst(firstResult);
        userRolesBodyRepresentation.setMax(maxResults);
        return getBaseQuery(realm, userRolesBodyRepresentation).map(UserRoleModel::getUser);
    }

    @SuppressWarnings("unchecked")
    private Stream<UserRoleModel> getBaseQuery(RealmModel realm, UserRolesBodyRepresentation representation) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<UserEntity> queryBuilder = builder.createQuery(UserEntity.class);
        From<?, UserEntity> from;

        List<Predicate> predicates = new ArrayList<>();

        // Handle Table Base
        // Setup Organizations if any.
        if (representation.getOrgId() != null) {
            // First, get the organization to get its group ID
            OrganizationEntity organization = em.find(OrganizationEntity.class, representation.getOrgId());
            if (organization != null) {
                Root<UserGroupMembershipEntity> membershipRoot = queryBuilder.from(UserGroupMembershipEntity.class);
                Join<UserGroupMembershipEntity, UserEntity> userJoin = membershipRoot.join("user");
                predicates.add(builder.equal(membershipRoot.get("groupId"), organization.getGroupId()));
                predicates.add(builder.equal(userJoin.get("realmId"), realm.getId()));
                from = userJoin;
            } else {
                from = queryBuilder.from(UserEntity.class);
            }
        } else {
            from = queryBuilder.from(UserEntity.class);
        }
        // End of Table Base.

        // Setup Role join.
        final Map<String, List<RoleModel>> rolesByUserId;
        if (representation.getClientId() != null || !representation.getByRoles().isEmpty()) {
            // Client UUID can be empty string if you want to retrieve all users
            String clientUUID = representation.getClientId() == null || representation.getClientId().isBlank() ? "" :
                getIdFromClientId(realm, representation.getClientId());
            Map<String, RoleModel> acceptedRoles = getRoles(realm, clientUUID);
            rolesByUserId = getUsersTiedToRoles(acceptedRoles);
            // If roleIds is populated, we remove any user's ids from being included if they do not have the role in question,
            // to prevent users that do not own this role from appearing.
            if (!representation.getByRoles().isEmpty()) {
                Set<String> userIds = rolesByUserId.entrySet()
                    .stream()
                    .filter(rolesByUser -> rolesByUser.getValue().stream().anyMatch(role -> representation.getByRoles().contains(role.getId())))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
                predicates.add(from.get("id").in(userIds));
            }
        } else {
            rolesByUserId = new HashMap<>();
        }
        // End of Role join.

        // Add baseline predicates
        predicates.add(builder.equal(from.get("realmId"), realm.getId()));
        Map<String, List<String>> customLongValueSearchAttributes = new HashMap<>();
        predicates.addAll(predicates(representation.getFilterAttributes(), from, customLongValueSearchAttributes));
        // End of baseline predicates


        // Add group permissions if needed
        Set<String> userGroups = (Set<String>) session.getAttribute(UserModel.GROUPS);
        if (userGroups != null) {
            groupsWithPermissionsSubquery(queryBuilder, userGroups, from, predicates);
        }
        // End of Group Permission

        queryBuilder.select(from)
            // Add all predicates within the CriteriaQuery.
            .where(predicates.toArray(Predicate[]::new))
            // Create ordering based on orderByField (If it starts with -, its descending based field).
            .orderBy(representation.getSortBy()
                .stream()
                .map(orderByField -> orderByField.startsWith("-") ? builder.desc(from.get(orderByField.substring(1))) :
                    builder.asc(from.get(orderByField)))
                .toList());

        TypedQuery<UserEntity> query = em.createQuery(queryBuilder);
        UserProvider users = session.users();
        return closing(paginateQuery(query, representation.getFirst(), representation.getMax()).getResultStream())
            // the following check verifies that there are no collisions with hashes
            .filter(predicateForMultiFilteringUsersByAttributes(customLongValueSearchAttributes, JpaHashUtils::compareSourceValueLowerCase))
            .map(userEntity -> {
                UserModel user = users.getUserById(realm, userEntity.getId());
                if (user == null) return null;

                UserRoleModel userRoleModel = new UserRoleModel(user);
                if (!rolesByUserId.isEmpty()) {
                    // Separated client roles from builder for clarity-sake.
                    Map<String, List<RoleModel>> userRoles = rolesByUserId.getOrDefault(user.getId(), Collections.emptyList())
                        .stream()
                        .collect(Collectors.groupingBy(role -> role.isClientRole() ? ((ClientModel) role.getContainer()).getClientId() : "",
                            Collectors.mapping(role -> role, Collectors.toList())));
                    userRoleModel.setRoles(userRoles);
                }
                return userRoleModel;
            }).filter(Objects::nonNull);
    }

    @SuppressWarnings("unchecked")
    private void groupsWithPermissionsSubquery(CriteriaQuery<?> query, Set<String> groupIds, From<?, UserEntity> root, List<Predicate> restrictions) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        Subquery subquery = query.subquery(String.class);

        Root<UserGroupMembershipEntity> from = subquery.from(UserGroupMembershipEntity.class);

        subquery.select(cb.literal(1));

        List<Predicate> subPredicates = new ArrayList<>();

        subPredicates.add(from.get("groupId").in(groupIds));
        subPredicates.add(cb.equal(from.get("user").get("id"), root.get("id")));

        Subquery subquery1 = query.subquery(String.class);

        subquery1.select(cb.literal(1));
        Root from1 = subquery1.from(ResourceEntity.class);

        List<Predicate> subs = new ArrayList<>();

        Expression<String> groupId = from.get("groupId");
        subs.add(cb.like(from1.get("name"), cb.concat("group.resource.", groupId)));

        subquery1.where(subs.toArray(Predicate[]::new));

        subPredicates.add(cb.exists(subquery1));

        subquery.where(subPredicates.toArray(Predicate[]::new));

        restrictions.add(cb.exists(subquery));
    }

    private String getIdFromClientId(RealmModel realm, String clientId) {
        CriteriaBuilder clientCb = em.getCriteriaBuilder();
        CriteriaQuery<ClientEntity> clientCq = clientCb.createQuery(ClientEntity.class);
        Root<ClientEntity> clientRoot = clientCq.from(ClientEntity.class);
        clientCq.where(clientCb.and(clientCb.equal(clientRoot.get("clientId"), clientId), clientCb.equal(clientRoot.get("realmId"), realm.getId())));

        return em.createQuery(clientCq).getResultStream().findFirst().map(ClientEntity::getId).orElse(null);
    }

    /**
     * [JD] Roles are missing some Foreign keys, missing two of them:
     * <ul><li>Role within UserRoleMappingEntity.</li>
     * <li>Roles within UserEntity.</li></ul>
     * <br>So for this function, we will be retrieving Client Roles
     * (Service roles are not supported at the moment due to lack of a need) tied to the clientId you passed.
     *
     * @param realm      The realm
     * @param clientUUID The Client UUID that you want to retrieve the roles from.
     * @return A Map of RoleEntities, tied by RoleEntity::getId.
     */
    private Map<String, RoleModel> getRoles(RealmModel realm, String clientUUID) {
        CriteriaBuilder roleCb = em.getCriteriaBuilder();
        CriteriaQuery<RoleEntity> roleCq = roleCb.createQuery(RoleEntity.class);
        Root<RoleEntity> roleRoot = roleCq.from(RoleEntity.class);
        if (clientUUID.isEmpty()) {
            roleCq.where(roleCb.and(roleCb.notEqual(roleRoot.get("clientId"), ""), roleCb.isNotNull(roleRoot.get("clientId"))));
        } else {
            roleCq.where(roleCb.equal(roleRoot.get("clientId"), clientUUID));
        }

        RoleProvider roleProvider = session.roles();
        return em.createQuery(roleCq)
            .getResultStream()
            .collect(Collectors.toMap(RoleEntity::getId, roleEntity -> roleProvider.getRoleById(realm, roleEntity.getId())));
    }

    /**
     * [JD] Roles are missing some Foreign keys, missing two of them:
     * <ul><li>Role within UserRoleMappingEntity.</li>
     * <li>Roles within UserEntity.</li></ul>
     * <br>So for this function, we will be retrieving a map of user's id tied to his respective roles using valid
     * roleIds (acquired from the roles Map you provided).
     *
     * @param roles A Map of `key: roleId | value: RoleModel`
     * @return A Map of `key: userId | value: List of RoleModel`
     */
    private Map<String, List<RoleModel>> getUsersTiedToRoles(Map<String, RoleModel> roles) {
        CriteriaBuilder userRoleCb = em.getCriteriaBuilder();
        CriteriaQuery<UserRoleMappingEntity> userRoleCq = userRoleCb.createQuery(UserRoleMappingEntity.class);
        Root<UserRoleMappingEntity> userRoleRoot = userRoleCq.from(UserRoleMappingEntity.class);

        userRoleCq.where(userRoleRoot.get("roleId").in(roles.keySet()));

        return em.createQuery(userRoleCq)
            .getResultStream()
            .collect(
                Collectors.groupingBy(urm -> urm.getUser().getId(), Collectors.mapping(user -> roles.get(user.getRoleId()), Collectors.toList())));
    }
}
