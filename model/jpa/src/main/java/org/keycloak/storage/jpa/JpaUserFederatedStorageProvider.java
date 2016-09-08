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
package org.keycloak.storage.jpa;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FederatedCredentials;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.federated.UserAttributeFederatedStorage;
import org.keycloak.storage.federated.UserBrokerLinkFederatedStorage;
import org.keycloak.storage.federated.UserConsentFederatedStorage;
import org.keycloak.storage.federated.UserCredentialsFederatedStorage;
import org.keycloak.storage.federated.UserFederatedStorageProvider;
import org.keycloak.storage.federated.UserGroupMembershipFederatedStorage;
import org.keycloak.storage.federated.UserRequiredActionsFederatedStorage;
import org.keycloak.storage.federated.UserRoleMappingsFederatedStorage;
import org.keycloak.storage.jpa.entity.BrokerLinkEntity;
import org.keycloak.storage.jpa.entity.FederatedUserAttributeEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentProtocolMapperEntity;
import org.keycloak.storage.jpa.entity.FederatedUserConsentRoleEntity;
import org.keycloak.storage.jpa.entity.FederatedUserCredentialEntity;
import org.keycloak.storage.jpa.entity.FederatedUserGroupMembershipEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRequiredActionEntity;
import org.keycloak.storage.jpa.entity.FederatedUserRoleMappingEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaUserFederatedStorageProvider implements
        UserFederatedStorageProvider,
        UserAttributeFederatedStorage,
        UserBrokerLinkFederatedStorage,
        UserConsentFederatedStorage,
        UserCredentialsFederatedStorage,
        UserGroupMembershipFederatedStorage,
        UserRequiredActionsFederatedStorage,
        UserRoleMappingsFederatedStorage {

    private final KeycloakSession session;
    protected EntityManager em;

    public JpaUserFederatedStorageProvider(KeycloakSession session, EntityManager em) {
        this.session = session;
        this.em = em;
    }

    @Override
    public void close() {

    }


    @Override
    public void setAttribute(RealmModel realm, UserModel user, String name, List<String> values) {
        deleteAttribute(realm, user, name);
        em.flush();
        for (String value : values) {
            persistAttributeValue(realm, user, name, value);
        }
    }

    private void deleteAttribute(RealmModel realm, UserModel user, String name) {
        em.createNamedQuery("deleteUserFederatedAttributesByUserAndName")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .setParameter("name", name)
                .executeUpdate();
    }

    private void persistAttributeValue(RealmModel realm, UserModel user, String name, String value) {
        FederatedUserAttributeEntity attr = new FederatedUserAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setUserId(user.getId());
        attr.setRealmId(realm.getId());
        attr.setStorageProviderId(StorageId.resolveProviderId(user));
        em.persist(attr);
    }

    @Override
    public void setSingleAttribute(RealmModel realm, UserModel user, String name, String value) {
        deleteAttribute(realm, user, name);
        em.flush();
        persistAttributeValue(realm, user, name, value);
    }

    @Override
    public void removeAttribute(RealmModel realm, UserModel user, String name) {
        deleteAttribute(realm, user, name);
        em.flush();
    }

    @Override
    public MultivaluedHashMap<String, String> getAttributes(RealmModel realm, UserModel user) {
        TypedQuery<FederatedUserAttributeEntity> query = em.createNamedQuery("getFederatedAttributesByUser", FederatedUserAttributeEntity.class);
        List<FederatedUserAttributeEntity> list = query
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .getResultList();
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (FederatedUserAttributeEntity entity : list) {
            result.add(entity.getName(), entity.getValue());

        }
        return result;
    }

    @Override
    public List<String> getUsersByUserAttribute(RealmModel realm, String name, String value) {
        TypedQuery<String> query = em.createNamedQuery("getFederatedAttributesByNameAndValue", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("name", name)
                .setParameter("value", value);
        return query.getResultList();
    }

    @Override
    public String getUserByFederatedIdentity(FederatedIdentityModel link, RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("findUserByBrokerLinkAndRealm", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("identityProvider", link.getIdentityProvider())
                .setParameter("brokerUserId", link.getUserId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        } else if (results.size() > 1) {
            throw new IllegalStateException("More results found for identityProvider=" + link.getIdentityProvider() +
                    ", userId=" + link.getUserId() + ", results=" + results);
        } else {
            return results.get(0);
        }
    }

    @Override
    public void addFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel link) {
        BrokerLinkEntity entity = new BrokerLinkEntity();
        entity.setRealmId(realm.getId());
        entity.setUserId(user.getId());
        entity.setBrokerUserId(link.getUserId());
        entity.setIdentityProvider(link.getIdentityProvider());
        entity.setToken(link.getToken());
        entity.setBrokerUserName(link.getUserName());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        em.persist(entity);

    }

    @Override
    public boolean removeFederatedIdentity(RealmModel realm, UserModel user, String socialProvider) {
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, user, socialProvider);
        if (entity == null) return false;
        em.remove(entity);
        return true;
    }

    private BrokerLinkEntity getBrokerLinkEntity(RealmModel realm, UserModel user, String socialProvider) {
        TypedQuery<BrokerLinkEntity> query = em.createNamedQuery("findBrokerLinkByUserAndProvider", BrokerLinkEntity.class)
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .setParameter("identityProvider", socialProvider);
        List<BrokerLinkEntity> results = query.getResultList();
        return results.size() > 0 ? results.get(0) : null;
    }

    @Override
    public void updateFederatedIdentity(RealmModel realm, UserModel user, FederatedIdentityModel model) {
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, user, model.getIdentityProvider());
        if (entity == null) return;
        entity.setBrokerUserName(model.getUserName());
        entity.setBrokerUserId(model.getUserId());
        entity.setToken(model.getToken());
        em.persist(entity);
        em.flush();

    }

    @Override
    public Set<FederatedIdentityModel> getFederatedIdentities(UserModel user, RealmModel realm) {
        TypedQuery<BrokerLinkEntity> query = em.createNamedQuery("findBrokerLinkByUser", BrokerLinkEntity.class)
                .setParameter("userId", user.getId());
        List<BrokerLinkEntity> results = query.getResultList();
        Set<FederatedIdentityModel> set = new HashSet<>();
        for (BrokerLinkEntity entity : results) {
            FederatedIdentityModel model = new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
            set.add(model);
        }
        return set;
    }

    @Override
    public FederatedIdentityModel getFederatedIdentity(UserModel user, String socialProvider, RealmModel realm) {
        BrokerLinkEntity entity = getBrokerLinkEntity(realm, user, socialProvider);
        if (entity == null) return null;
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getBrokerUserId(), entity.getBrokerUserName(), entity.getToken());
    }

    @Override
    public void addConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(user, clientId);
        if (consentEntity != null) {
            throw new ModelDuplicateException("Consent already exists for client [" + clientId + "] and user [" + user.getId() + "]");
        }

        consentEntity = new FederatedUserConsentEntity();
        consentEntity.setId(KeycloakModelUtils.generateId());
        consentEntity.setUserId(user.getId());
        consentEntity.setClientId(clientId);
        consentEntity.setRealmId(realm.getId());
        consentEntity.setStorageProviderId(StorageId.resolveProviderId(user));
        em.persist(consentEntity);
        em.flush();

        updateGrantedConsentEntity(consentEntity, consent);

    }

    @Override
    public UserConsentModel getConsentByClient(RealmModel realm, UserModel user, String clientInternalId) {
        FederatedUserConsentEntity entity = getGrantedConsentEntity(user, clientInternalId);
        return toConsentModel(realm, entity);
    }

    @Override
    public List<UserConsentModel> getConsents(RealmModel realm, UserModel user) {
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery("userFederatedConsentsByUser", FederatedUserConsentEntity.class);
        query.setParameter("userId", user.getId());
        List<FederatedUserConsentEntity> results = query.getResultList();

        List<UserConsentModel> consents = new ArrayList<UserConsentModel>();
        for (FederatedUserConsentEntity entity : results) {
            UserConsentModel model = toConsentModel(realm, entity);
            consents.add(model);
        }
        return consents;
    }

    @Override
    public void updateConsent(RealmModel realm, UserModel user, UserConsentModel consent) {
        String clientId = consent.getClient().getId();

        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(user, clientId);
        if (consentEntity == null) {
            throw new ModelException("Consent not found for client [" + clientId + "] and user [" + user.getId() + "]");
        }

        updateGrantedConsentEntity(consentEntity, consent);

    }

    @Override
    public boolean revokeConsentForClient(RealmModel realm, UserModel user, String clientInternalId) {
        FederatedUserConsentEntity consentEntity = getGrantedConsentEntity(user, clientInternalId);
        if (consentEntity == null) return false;

        em.remove(consentEntity);
        em.flush();
        return true;
    }

    private FederatedUserConsentEntity getGrantedConsentEntity(UserModel user, String clientId) {
        TypedQuery<FederatedUserConsentEntity> query = em.createNamedQuery("userFederatedConsentByUserAndClient", FederatedUserConsentEntity.class);
        query.setParameter("userId", user.getId());
        query.setParameter("clientId", clientId);
        List<FederatedUserConsentEntity> results = query.getResultList();
        if (results.size() > 1) {
            throw new ModelException("More results found for user [" + user.getUsername() + "] and client [" + clientId + "]");
        } else if (results.size() == 1) {
            return results.get(0);
        } else {
            return null;
        }
    }


    private UserConsentModel toConsentModel(RealmModel realm, FederatedUserConsentEntity entity) {
        if (entity == null) {
            return null;
        }

        ClientModel client = realm.getClientById(entity.getClientId());
        if (client == null) {
            throw new ModelException("Client with id " + entity.getClientId() + " is not available");
        }
        UserConsentModel model = new UserConsentModel(client);

        Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = entity.getGrantedRoles();
        if (grantedRoleEntities != null) {
            for (FederatedUserConsentRoleEntity grantedRole : grantedRoleEntities) {
                RoleModel grantedRoleModel = realm.getRoleById(grantedRole.getRoleId());
                if (grantedRoleModel != null) {
                    model.addGrantedRole(grantedRoleModel);
                }
            }
        }

        Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = entity.getGrantedProtocolMappers();
        if (grantedProtocolMapperEntities != null) {
            for (FederatedUserConsentProtocolMapperEntity grantedProtMapper : grantedProtocolMapperEntities) {
                ProtocolMapperModel protocolMapper = client.getProtocolMapperById(grantedProtMapper.getProtocolMapperId());
                model.addGrantedProtocolMapper(protocolMapper);
            }
        }

        return model;
    }

    // Update roles and protocolMappers to given consentEntity from the consentModel
    private void updateGrantedConsentEntity(FederatedUserConsentEntity consentEntity, UserConsentModel consentModel) {
        Collection<FederatedUserConsentProtocolMapperEntity> grantedProtocolMapperEntities = consentEntity.getGrantedProtocolMappers();
        Collection<FederatedUserConsentProtocolMapperEntity> mappersToRemove = new HashSet<>(grantedProtocolMapperEntities);

        for (ProtocolMapperModel protocolMapper : consentModel.getGrantedProtocolMappers()) {
            FederatedUserConsentProtocolMapperEntity grantedProtocolMapperEntity = new FederatedUserConsentProtocolMapperEntity();
            grantedProtocolMapperEntity.setUserConsent(consentEntity);
            grantedProtocolMapperEntity.setProtocolMapperId(protocolMapper.getId());

            // Check if it's already there
            if (!grantedProtocolMapperEntities.contains(grantedProtocolMapperEntity)) {
                em.persist(grantedProtocolMapperEntity);
                em.flush();
                grantedProtocolMapperEntities.add(grantedProtocolMapperEntity);
            } else {
                mappersToRemove.remove(grantedProtocolMapperEntity);
            }
        }
        // Those mappers were no longer on consentModel and will be removed
        for (FederatedUserConsentProtocolMapperEntity toRemove : mappersToRemove) {
            grantedProtocolMapperEntities.remove(toRemove);
            em.remove(toRemove);
        }

        Collection<FederatedUserConsentRoleEntity> grantedRoleEntities = consentEntity.getGrantedRoles();
        Set<FederatedUserConsentRoleEntity> rolesToRemove = new HashSet<>(grantedRoleEntities);
        for (RoleModel role : consentModel.getGrantedRoles()) {
            FederatedUserConsentRoleEntity consentRoleEntity = new FederatedUserConsentRoleEntity();
            consentRoleEntity.setUserConsent(consentEntity);
            consentRoleEntity.setRoleId(role.getId());

            // Check if it's already there
            if (!grantedRoleEntities.contains(consentRoleEntity)) {
                em.persist(consentRoleEntity);
                em.flush();
                grantedRoleEntities.add(consentRoleEntity);
            } else {
                rolesToRemove.remove(consentRoleEntity);
            }
        }
        // Those roles were no longer on consentModel and will be removed
        for (FederatedUserConsentRoleEntity toRemove : rolesToRemove) {
            grantedRoleEntities.remove(toRemove);
            em.remove(toRemove);
        }

        em.flush();
    }



    @Override
    public List<UserCredentialValueModel> getCredentials(RealmModel realm, UserModel user) {
        TypedQuery<FederatedUserCredentialEntity> query = em.createNamedQuery("federatedUserCredentialByUser", FederatedUserCredentialEntity.class)
                .setParameter("userId", user.getId());
        List<FederatedUserCredentialEntity> results = query.getResultList();
        List<UserCredentialValueModel> list = new LinkedList<>();
        for (FederatedUserCredentialEntity credEntity : results) {
            UserCredentialValueModel credModel = new UserCredentialValueModel();
            credModel.setId(credEntity.getId());
            credModel.setType(credEntity.getType());
            credModel.setDevice(credEntity.getDevice());
            credModel.setValue(credEntity.getValue());
            credModel.setCreatedDate(credEntity.getCreatedDate());
            credModel.setSalt(credEntity.getSalt());
            credModel.setHashIterations(credEntity.getHashIterations());
            credModel.setCounter(credEntity.getCounter());
            credModel.setAlgorithm(credEntity.getAlgorithm());
            credModel.setDigits(credEntity.getDigits());
            credModel.setPeriod(credEntity.getPeriod());

            list.add(credModel);
        }
        return list;
    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, UserCredentialModel cred) {
        FederatedCredentials.updateCredential(session, this, realm, user, cred);

    }

    @Override
    public void updateCredential(RealmModel realm, UserModel user, UserCredentialValueModel cred) {
        FederatedUserCredentialEntity entity = null;
        if (cred.getId() != null) entity = em.find(FederatedUserCredentialEntity.class, cred.getId());
        boolean newEntity = false;
        if (entity == null) {
            entity = new FederatedUserCredentialEntity();
            entity.setId(KeycloakModelUtils.generateId());
            newEntity = true;
        }
        entity.setUserId(user.getId());
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        entity.setAlgorithm(cred.getAlgorithm());
        entity.setCounter(cred.getCounter());
        Long createdDate = cred.getCreatedDate();
        if (createdDate == null) createdDate = System.currentTimeMillis();
        entity.setCreatedDate(createdDate);
        entity.setDevice(cred.getDevice());
        entity.setDigits(cred.getDigits());
        entity.setHashIterations(cred.getHashIterations());
        entity.setPeriod(cred.getPeriod());
        entity.setSalt(cred.getSalt());
        entity.setType(cred.getType());
        entity.setValue(cred.getValue());
        if (newEntity) {
            em.persist(entity);
        }

    }

    @Override
    public void removeCredential(RealmModel realm, UserModel user, UserCredentialValueModel cred) {
        FederatedUserCredentialEntity entity = em.find(FederatedUserCredentialEntity.class, cred.getId());
        em.remove(entity);
    }

    @Override
    public Set<GroupModel> getGroups(RealmModel realm, UserModel user) {
        Set<GroupModel> set = new HashSet<>();
        TypedQuery<FederatedUserGroupMembershipEntity> query = em.createNamedQuery("feduserGroupMembership", FederatedUserGroupMembershipEntity.class);
        query.setParameter("userId", user.getId());
        List<FederatedUserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return set;
        for (FederatedUserGroupMembershipEntity entity : results) {
            GroupModel group = realm.getGroupById(entity.getGroupId());
            set.add(group);
        }
        return set;
    }

    @Override
    public void joinGroup(RealmModel realm, UserModel user, GroupModel group) {
        if (isMemberOf(realm, user, group)) return;
        FederatedUserGroupMembershipEntity entity = new FederatedUserGroupMembershipEntity();
        entity.setUserId(user.getId());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        entity.setGroupId(group.getId());
        entity.setRealmId(realm.getId());
        em.persist(entity);

    }

    public boolean isMemberOf(RealmModel realm, UserModel user, GroupModel group) {
        Set<GroupModel> roles = user.getGroups();
        return KeycloakModelUtils.isMember(roles, group);
    }


    @Override
    public void leaveGroup(RealmModel realm, UserModel user, GroupModel group) {
        if (user == null || group == null) return;

        TypedQuery<FederatedUserGroupMembershipEntity> query1 = em.createNamedQuery("feduserMemberOf", FederatedUserGroupMembershipEntity.class);
        query1.setParameter("userId", user.getId());
        query1.setParameter("groupId", group.getId());
        TypedQuery<FederatedUserGroupMembershipEntity> query = query1;
        List<FederatedUserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (FederatedUserGroupMembershipEntity entity : results) {
            em.remove(entity);
        }
        em.flush();

    }

    @Override
    public List<String> getMembership(RealmModel realm, GroupModel group, int firstResult, int max) {
        TypedQuery<String> query = em.createNamedQuery("fedgroupMembership", String.class)
                .setParameter("realmId", realm.getId())
                .setParameter("groupId", group.getId());
        query.setFirstResult(firstResult);
        query.setMaxResults(max);
        return query.getResultList();
    }

    @Override
    public Set<String> getRequiredActions(RealmModel realm, UserModel user) {
        Set<String> set = new HashSet<>();
        List<FederatedUserRequiredActionEntity> values = getRequiredActionEntities(realm, user);
        for (FederatedUserRequiredActionEntity entity : values) {
            set.add(entity.getAction());
        }

        return set;

    }

    private List<FederatedUserRequiredActionEntity> getRequiredActionEntities(RealmModel realm, UserModel user) {
        TypedQuery<FederatedUserRequiredActionEntity> query = em.createNamedQuery("getFederatedUserRequiredActionsByUser", FederatedUserRequiredActionEntity.class)
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId());
        return query.getResultList();
    }

    @Override
    public void addRequiredAction(RealmModel realm, UserModel user, String action) {
        if (user.getRequiredActions().contains(action)) return;
        FederatedUserRequiredActionEntity entity = new FederatedUserRequiredActionEntity();
        entity.setUserId(user.getId());
        entity.setRealmId(realm.getId());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        entity.setAction(action);
        em.persist(entity);

    }

    @Override
    public void removeRequiredAction(RealmModel realm, UserModel user, String action) {
        List<FederatedUserRequiredActionEntity> values = getRequiredActionEntities(realm, user);
        for (FederatedUserRequiredActionEntity entity : values) {
            if (action.equals(entity.getAction())) em.remove(entity);
        }
        em.flush();

    }

    @Override
    public void grantRole(RealmModel realm, UserModel user, RoleModel role) {
        if (user.hasRole(role)) return;
        FederatedUserRoleMappingEntity entity = new FederatedUserRoleMappingEntity();
        entity.setUserId(user.getId());
        entity.setStorageProviderId(StorageId.resolveProviderId(user));
        entity.setRealmId(realm.getId());
        entity.setRoleId(role.getId());
        em.persist(entity);

    }

    @Override
    public Set<RoleModel> getRoleMappings(RealmModel realm, UserModel user) {
        Set<RoleModel> set = new HashSet<>();
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", user.getId());
        List<FederatedUserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return set;
        for (FederatedUserRoleMappingEntity entity : results) {
            RoleModel role = realm.getRoleById(entity.getRoleId());
            set.add(role);
        }
        return set;
    }

    @Override
    public void deleteRoleMapping(RealmModel realm, UserModel user, RoleModel role) {
        TypedQuery<FederatedUserRoleMappingEntity> query = em.createNamedQuery("feduserRoleMappings", FederatedUserRoleMappingEntity.class);
        query.setParameter("userId", user.getId());
        List<FederatedUserRoleMappingEntity> results = query.getResultList();
        for (FederatedUserRoleMappingEntity entity : results) {
            if (entity.getRoleId().equals(role.getId())) em.remove(entity);

        }
        em.flush();
    }

    @Override
    public void preRemove(RealmModel realm) {
        int num = em.createNamedQuery("deleteFederatedUserConsentRolesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserConsentProtMappersByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserConsentsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRoleMappingsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRequiredActionsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteBrokerLinkByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserCredentialsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteUserFederatedAttributesByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserGroupMembershipByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, UserFederationProviderModel link) {
        int num = em.createNamedQuery("deleteFederatedUserRoleMappingsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserRequiredActionsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteBrokerLinkByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteFederatedUserCredentialsByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
        num = em.createNamedQuery("deleteUserFederatedAttributesByRealmAndLink")
                .setParameter("realmId", realm.getId())
                .setParameter("link", link.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, RoleModel role) {
        em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, GroupModel group) {
        em.createNamedQuery("deleteFederatedUserGroupMembershipsByGroup").setParameter("groupId", group.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, ClientModel client) {
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentRolesByClient").setParameter("clientId", client.getId()).executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByClient").setParameter("clientId", client.getId()).executeUpdate();
    }

    @Override
    public void preRemove(ProtocolMapperModel protocolMapper) {
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByProtocolMapper")
                .setParameter("protocolMapperId", protocolMapper.getId())
                .executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm, UserModel user) {
        em.createNamedQuery("deleteBrokerLinkByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteUserFederatedAttributesByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentRolesByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserCredentialByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserGroupMembershipsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRequiredActionsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByUser")
                .setParameter("userId", user.getId())
                .setParameter("realmId", realm.getId())
                .executeUpdate();

    }

    @Override
    public void preRemove(RealmModel realm, ComponentModel model) {
        if (!model.getProviderType().equals(UserStorageProvider.class.getName())) return;

        em.createNamedQuery("deleteBrokerLinkByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedAttributesByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentProtMappersByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserConsentsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserCredentialsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserGroupMembershipByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRequiredActionsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();
        em.createNamedQuery("deleteFederatedUserRoleMappingsByStorageProvider")
                .setParameter("storageProviderId", model.getId())
                .executeUpdate();

    }
}
