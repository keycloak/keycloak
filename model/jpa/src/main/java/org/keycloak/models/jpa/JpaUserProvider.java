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

import org.keycloak.models.ClientModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.FederatedIdentityEntity;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserProvider implements UserProvider {

    private static final String EMAIL = "email";
    private static final String USERNAME = "username";
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";

    private final KeycloakSession session;
    protected EntityManager em;

    public JpaUserProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
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
            for (String r : realm.getDefaultRoles()) {
                userModel.grantRoleImpl(realm.getRole(r)); // No need to check if user has role as it's new user
            }

            for (ClientModel application : realm.getClients()) {
                for (String r : application.getDefaultRoles()) {
                    userModel.grantRoleImpl(application.getRole(r)); // No need to check if user has role as it's new user
                }
            }

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
        em.createNamedQuery("deleteUserConsentRolesByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentProtMappersByUser").setParameter("user", user).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByUser").setParameter("user", user).executeUpdate();
        em.flush();
        // not sure why i have to do a clear() here.  I was getting some messed up errors that Hibernate couldn't
        // un-delete the UserEntity.
        em.clear();
        user = em.find(UserEntity.class, id);
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
        FederatedIdentityEntity federatedIdentity = findFederatedIdentity(federatedUser, federatedIdentityModel.getIdentityProvider());

        federatedIdentity.setToken(federatedIdentityModel.getToken());

        em.persist(federatedIdentity);
        em.flush();
    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String identityProvider) {
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider);
        if (entity != null) {
            em.remove(entity);
            em.flush();
            return true;
        } else {
            return false;
        }
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
        int num = em.createNamedQuery("deleteUserConsentRolesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserConsentProtMappersByRealm")
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
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        int num = em.createNamedQuery("deleteUserRoleMappingsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteUserRequiredActionsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteFederatedIdentityByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteCredentialsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteUserAttributesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteUsersByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        em.createNamedQuery("deleteUserConsentRolesByRole").setParameter("roleId", role.getId()).executeUpdate();
        em.createNamedQuery("deleteUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        em.createNamedQuery("deleteUserConsentProtMappersByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteUserConsentRolesByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteUserConsentsByClient").setParameter("clientId", client.getId()).executeUpdate();
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        em.createNamedQuery("deleteUserConsentProtMappersByProtocolMapper")
                .setParameter("protocolMapperId", protocolMapper.getId())
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
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();

    }

    @Override
    public UserModel getUserById(String id, RealmModel realm) {
        TypedQuery<UserEntity> query = em.createNamedQuery("getRealmUserById", UserEntity.class);
        query.setParameter("id", id);
        query.setParameter("realmId", realm.getId());
        List<UserEntity> entities = query.getResultList();
        if (entities.size() == 0) return null;
        return new UserAdapter(session, realm, em, entities.get(0));
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
        return results.isEmpty() ? null : new UserAdapter(session, realm, em, results.get(0));
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
    public UserModel getUserByServiceAccountClient(ClientModel client) {
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
    public int getUsersCount(RealmModel realm) {
        Object count = em.createNamedQuery("getRealmUserCount")
                .setParameter("realmId", realm.getId())
                .getSingleResult();
        return ((Number)count).intValue();
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
        List<UserModel> users = new ArrayList<UserModel>();
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

        List<UserModel> users = new ArrayList<UserModel>();
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
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm) {
        return searchForUserByAttributes(attributes, realm, -1, -1);
    }

    @Override
    public List<UserModel> searchForUserByAttributes(Map<String, String> attributes, RealmModel realm, int firstResult, int maxResults) {
        StringBuilder builder = new StringBuilder("select u from UserEntity u where u.realmId = :realmId");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String attribute = null;
            String parameterName = null;
            if (entry.getKey().equals(UserModel.USERNAME)) {
                attribute = "lower(u.username)";
                parameterName = JpaUserProvider.USERNAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                attribute = "lower(u.firstName)";
                parameterName = JpaUserProvider.FIRST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                attribute = "lower(u.lastName)";
                parameterName = JpaUserProvider.LAST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                attribute = "lower(u.email)";
                parameterName = JpaUserProvider.EMAIL;
            }
            if (attribute == null) continue;
            builder.append(" and ");
            builder.append(attribute).append(" like :").append(parameterName);
        }
        builder.append(" order by u.username");
        String q = builder.toString();
        TypedQuery<UserEntity> query = em.createQuery(q, UserEntity.class);
        query.setParameter("realmId", realm.getId());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String parameterName = null;
            if (entry.getKey().equals(UserModel.USERNAME)) {
                parameterName = JpaUserProvider.USERNAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.FIRST_NAME)) {
                parameterName = JpaUserProvider.FIRST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.LAST_NAME)) {
                parameterName = JpaUserProvider.LAST_NAME;
            } else if (entry.getKey().equalsIgnoreCase(UserModel.EMAIL)) {
                parameterName = JpaUserProvider.EMAIL;
            }
            if (parameterName == null) continue;
            query.setParameter(parameterName, "%" + entry.getValue().toLowerCase() + "%");
        }
        if (firstResult != -1) {
            query.setFirstResult(firstResult);
        }
        if (maxResults != -1) {
            query.setMaxResults(maxResults);
        }
        List<UserEntity> results = query.getResultList();
        List<UserModel> users = new ArrayList<UserModel>();
        for (UserEntity entity : results) users.add(new UserAdapter(session, realm, em, entity));
        return users;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        TypedQuery<UserAttributeEntity> query = em.createNamedQuery("getAttributesByNameAndValue", UserAttributeEntity.class);
        query.setParameter("name", attrName);
        query.setParameter("value", attrValue);
        List<UserAttributeEntity> results = query.getResultList();

        List<UserModel> users = new ArrayList<UserModel>();
        for (UserAttributeEntity attr : results) {
            UserEntity user = attr.getUser();
            users.add(new UserAdapter(session, realm, em, user));
        }
        return users;
    }

    private FederatedIdentityEntity findFederatedIdentity(UserModel user, String identityProvider) {
        TypedQuery<FederatedIdentityEntity> query = em.createNamedQuery("findFederatedIdentityByUserAndProvider", FederatedIdentityEntity.class);
        UserEntity userEntity = em.getReference(UserEntity.class, user.getId());
        query.setParameter("user", userEntity);
        query.setParameter("identityProvider", identityProvider);
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
        FederatedIdentityEntity entity = findFederatedIdentity(user, identityProvider);
        return (entity != null) ? new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken()) : null;
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
}
